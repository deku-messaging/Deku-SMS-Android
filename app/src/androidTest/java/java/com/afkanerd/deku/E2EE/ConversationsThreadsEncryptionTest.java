package java.com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityAES;
import com.google.i18n.phonenumbers.NumberParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class ConversationsThreadsEncryptionTest {

    Context context;
    String address = "+237612345678";
    int sessionNumber = 0;
    String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=";

    public ConversationsThreadsEncryptionTest() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void getKeyStoreAliasTest() throws NumberParseException {
        String outputKeystoreAlias = E2EEHandler.deriveKeystoreAlias(address, sessionNumber);
        assertEquals(keystoreAlias, outputKeystoreAlias);
    }

    @Test
    public void getAddressFromKeystore() throws NumberParseException {
        String keystoreAlias = E2EEHandler.deriveKeystoreAlias(address, 0);
        String derivedAddress = E2EEHandler.getAddressFromKeystore(keystoreAlias);

        assertEquals(address, derivedAddress);
    }

    @Test
    public void testCanCreateAndRemoveKeyPair() throws GeneralSecurityException, IOException, InterruptedException {
        E2EEHandler.createNewKeyPair(context, keystoreAlias);
        assertTrue(E2EEHandler.isAvailableInKeystore(keystoreAlias));

        E2EEHandler.removeFromKeystore(context, keystoreAlias);
        assertFalse(E2EEHandler.isAvailableInKeystore(keystoreAlias));
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testSize() throws GeneralSecurityException, IOException, InterruptedException {
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        int length = publicKey.getEncoded().length;

        assertEquals(91, length);
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testIsValidAgreementPubKey() throws GeneralSecurityException, IOException, InterruptedException {
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        byte[] dekuPublicKey = E2EEHandler.buildDekuPublicKey(publicKey.getEncoded());
        assertTrue(E2EEHandler.isValidDefaultPublicKey(dekuPublicKey));

        SecretKey secretKey = SecurityAES.generateSecretKey(91);
        byte[] invalidPublicKey = E2EEHandler.buildDekuPublicKey(secretKey.getEncoded());
        assertFalse(E2EEHandler.isValidDefaultPublicKey(invalidPublicKey));
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testBuildForEncryptionRequest() throws GeneralSecurityException, NumberParseException, IOException, InterruptedException {
        byte[] transmissionRequest = E2EEHandler.buildForEncryptionRequest(context, address);
        assertTrue(E2EEHandler.isValidDefaultPublicKey(transmissionRequest));
    }
    @Test
    public void canBeTransmittedAsData() throws GeneralSecurityException, NumberParseException, IOException, InterruptedException {
        byte[] transmissionRequest = E2EEHandler.buildForEncryptionRequest(context, address);
        assertTrue(transmissionRequest.length < 120);
    }

    @Test
    public void canDoubleRatchet() throws Throwable {
        KeystoreHelpers.removeAllFromKeystore(context);
        String aliceAddress = "+237612345678";
        String bobAddress = "+237612345670";

        byte[] aliceTransmissionKey = E2EEHandler.buildForEncryptionRequest(context, bobAddress);

        // bob received alice's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(aliceTransmissionKey));
        byte[] aliceExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(aliceTransmissionKey);
        String aliceKeystoreAlias = E2EEHandler.deriveKeystoreAlias(aliceAddress, 0);
        assertEquals(E2EEHandler.REQUEST_KEY,
                E2EEHandler.getKeyType(context, aliceKeystoreAlias, null));
        E2EEHandler.insertNewPeerPublicKey(context, aliceExtractedTransmissionKey,
                aliceKeystoreAlias);

        byte[] bobTransmissionKey = E2EEHandler.buildForEncryptionRequest(context, aliceAddress);

        // alice received bob's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(bobTransmissionKey));
        byte[] bobExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(bobTransmissionKey);
        String bobKeystoreAlias = E2EEHandler.deriveKeystoreAlias(bobAddress, 0);
        assertEquals(E2EEHandler.AGREEMENT_KEY,
                E2EEHandler.getKeyType(context, bobKeystoreAlias, bobExtractedTransmissionKey));
        E2EEHandler.insertNewAgreementKey(context, bobExtractedTransmissionKey, bobKeystoreAlias);
        assertEquals(E2EEHandler.IGNORE_KEY,
                E2EEHandler.getKeyType(context, bobKeystoreAlias, bobExtractedTransmissionKey));

        assertTrue(E2EEHandler.isAvailableInKeystore(aliceKeystoreAlias));
        assertTrue(E2EEHandler.isAvailableInKeystore(bobKeystoreAlias));

        assertTrue(E2EEHandler.canCommunicateSecurely(context, aliceKeystoreAlias));
        assertTrue(E2EEHandler.canCommunicateSecurely(context, bobKeystoreAlias));

//        String aliceText = "Hello world!";
        final byte[] aliceText = CryptoHelpers.generateRandomBytes(130);
        byte[] aliceCipherText = E2EEHandler.encrypt(context, bobKeystoreAlias, aliceText);
        String aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText);
        // ----> alice sends the message

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDefaultText(aliceTransmissionText));
        byte[] aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);

        byte[] alicePlainText = E2EEHandler.decrypt(context, aliceKeystoreAlias, aliceExtractedText);
        assertArrayEquals(aliceText, alicePlainText);
    }

    @Test
    public void canE2EE() throws Exception {
        KeystoreHelpers.removeAllFromKeystore(context);
        String aliceAddress = "+237612345678";
        String bobAddress = "+237612345670";

        byte[] aliceTransmissionKey = E2EEHandler.buildForEncryptionRequest(context, bobAddress);

        // bob received alice's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(aliceTransmissionKey));
        byte[] aliceExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(aliceTransmissionKey);
        String aliceKeystoreAlias = E2EEHandler.deriveKeystoreAlias(aliceAddress, 0);
        assertEquals(E2EEHandler.REQUEST_KEY,
                E2EEHandler.getKeyType(context, aliceKeystoreAlias, null));
        E2EEHandler.insertNewPeerPublicKey(context, aliceExtractedTransmissionKey,
                aliceKeystoreAlias);

        byte[] bobTransmissionKey = E2EEHandler.buildForEncryptionRequest(context, aliceAddress);

        // alice received bob's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(bobTransmissionKey));
        byte[] bobExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(bobTransmissionKey);
        String bobKeystoreAlias = E2EEHandler.deriveKeystoreAlias(bobAddress, 0);
        assertEquals(E2EEHandler.AGREEMENT_KEY,
                E2EEHandler.getKeyType(context, bobKeystoreAlias, bobExtractedTransmissionKey));
        E2EEHandler.insertNewPeerPublicKey(context, bobExtractedTransmissionKey, bobKeystoreAlias);
        assertEquals(E2EEHandler.IGNORE_KEY,
                E2EEHandler.getKeyType(context, bobKeystoreAlias, bobExtractedTransmissionKey));

        assertTrue(E2EEHandler.isAvailableInKeystore(aliceKeystoreAlias));
        assertTrue(E2EEHandler.isAvailableInKeystore(bobKeystoreAlias));

        assertTrue(E2EEHandler.canCommunicateSecurely(context, aliceKeystoreAlias));
        assertTrue(E2EEHandler.canCommunicateSecurely(context, bobKeystoreAlias));


//        final byte[] plainText = CryptoHelpers.generateRandomBytes(130);
        String aliceText = "Hello world!";
        byte[] aliceCipherText = E2EEHandler.encryptText(context, bobKeystoreAlias, aliceText);
        String aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText);
        // ----> alice sends the message

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDefaultText(aliceTransmissionText));
        byte[] aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);

        byte[] alicePlainText = E2EEHandler.decryptText(context, aliceKeystoreAlias, aliceExtractedText);
        assertEquals(aliceText, new String(alicePlainText));
    }

}
