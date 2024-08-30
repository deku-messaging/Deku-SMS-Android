package java.com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Pair;

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
    public ConversationsThreadsEncryptionTest() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void getKeyStoreAliasTest() throws NumberParseException {
        String address = "+237111111111";
        int sessionNumber = 0;
        String keystoreAlias = "MjM3MTExMTExMTExXzA=";
        String outputKeystoreAlias = E2EEHandler.deriveKeystoreAlias(address, sessionNumber);
        assertEquals(keystoreAlias, outputKeystoreAlias);
    }

    @Test
    public void getAddressFromKeystore() throws NumberParseException {
        String address = "+237222222222";
        String keystoreAlias = E2EEHandler.deriveKeystoreAlias(address, 0);
        String derivedAddress = E2EEHandler.getAddressFromKeystore(keystoreAlias);

        assertEquals(address, derivedAddress);
    }

    @Test
    public void testCanCreateAndRemoveKeyPair() throws GeneralSecurityException, IOException, InterruptedException {
        String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=";
        E2EEHandler.createNewKeyPair(context, keystoreAlias);
        assertTrue(E2EEHandler.isAvailableInKeystore(keystoreAlias));

        E2EEHandler.removeFromKeystore(context, keystoreAlias);
        assertFalse(E2EEHandler.isAvailableInKeystore(keystoreAlias));
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testSize() throws GeneralSecurityException, IOException, InterruptedException {
        String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=TS";
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        int length = publicKey.getEncoded().length;

        assertEquals(91, length);
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testIsValidAgreementPubKey() throws GeneralSecurityException, IOException, InterruptedException {
        String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=VA";
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        byte[] dekuPublicKey = E2EEHandler.buildDefaultPublicKey(publicKey.getEncoded());
        assertTrue(E2EEHandler.isValidDefaultPublicKey(dekuPublicKey));

        SecretKey secretKey = SecurityAES.generateSecretKey(91);
        byte[] invalidPublicKey = E2EEHandler.buildDefaultPublicKey(secretKey.getEncoded());
        assertFalse(E2EEHandler.isValidDefaultPublicKey(invalidPublicKey));
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void testBuildForEncryptionRequest() throws Exception {
        String address = "+237333333333";
        byte[] transmissionRequest = E2EEHandler
                .buildForEncryptionRequest(context, address, null).second;
        assertTrue(E2EEHandler.isValidDefaultPublicKey(transmissionRequest));
    }
    @Test
    public void canBeTransmittedAsData() throws Exception {
        String address = "+237444444444";
        byte[] transmissionRequest = E2EEHandler
                .buildForEncryptionRequest(context, address, null).second;
        assertTrue(transmissionRequest.length < 120);
    }

    @Test
    public void canSelfDoubleRatchet() throws Throwable {
        String aliceAddress = "+2375555555550";
        String bobAddress = "+2375555555550";

        // Initial request
        Pair<String, byte[]> keystorePairAlice = E2EEHandler.buildForEncryptionRequest(context,
                bobAddress, null);
        String aliceKeystoreAlias = keystorePairAlice.first;
        byte[] aliceTransmissionKey = keystorePairAlice.second;

        // Bob received Alice's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(aliceTransmissionKey));
        byte[] aliceExtractedTransmissionKey =
                E2EEHandler.extractTransmissionKey(aliceTransmissionKey);
        String bobKeystoreAlias = E2EEHandler.deriveKeystoreAlias(aliceAddress, 0);
        E2EEHandler.insertNewAgreementKeyDefault(context, aliceExtractedTransmissionKey,
                bobKeystoreAlias);

        // assumption == bob initiates a reply to build key, but does not proceed to send
        final String bobKeystoreAliasSelf = E2EEHandler.buildForSelf(bobKeystoreAlias);
        Pair<String, byte[]> keystorePairBob = E2EEHandler.buildForEncryptionRequest(context,
                aliceAddress, bobKeystoreAliasSelf);
        byte[] bobTransmissionKey = keystorePairBob.second;
        assertTrue(E2EEHandler.isValidDefaultPublicKey(bobTransmissionKey));
        byte[] bobExtractedTransmissionKey =
                E2EEHandler.extractTransmissionKey(bobTransmissionKey);
        E2EEHandler.insertNewAgreementKeyDefault(context, bobExtractedTransmissionKey,
                bobKeystoreAliasSelf);

        // assumption == alice exist and self is true
        assertTrue(E2EEHandler.isSelf(context,
                E2EEHandler.deriveKeystoreAlias(aliceAddress, 0)));

        assertTrue(E2EEHandler.isAvailableInKeystore(aliceKeystoreAlias));
        assertTrue(E2EEHandler.isAvailableInKeystore(bobKeystoreAlias));

//        assertTrue(E2EEHandler.canCommunicateSecurely(context, aliceKeystoreAlias));
//        assertTrue(E2EEHandler.canCommunicateSecurely(context, bobKeystoreAlias));

        final boolean isSelf = E2EEHandler.isSelf(context,
                E2EEHandler.deriveKeystoreAlias(aliceAddress, 0));
        assertTrue(isSelf);
        // ----> alice sends the message
        byte[] aliceText = CryptoHelpers.generateRandomBytes(130);
        byte[][] aliceCipherText = E2EEHandler.encrypt(context, aliceKeystoreAlias, aliceText, true);
        String aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText[0]);

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDefaultText(aliceTransmissionText));
        byte[] aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);
        byte[] alicePlainText = E2EEHandler.decrypt(context, bobKeystoreAliasSelf,
                aliceExtractedText, null, null, true);
        assertArrayEquals(aliceText, alicePlainText);

        // <---- bob sends a message
        byte[] bobText = CryptoHelpers.generateRandomBytes(130);
        byte[][] bobCipherText = E2EEHandler.encrypt(context, bobKeystoreAlias, bobText, true);
        String bobTransmissionText = E2EEHandler.buildTransmissionText(bobCipherText[0]);

        // <---- then bob sends another
        byte[] bobText1 = CryptoHelpers.generateRandomBytes(130);
        byte[][] bobCipherText1 = E2EEHandler.encrypt(context, bobKeystoreAlias, bobText1, true);
        String bobTransmissionText1 = E2EEHandler.buildTransmissionText(bobCipherText1[0]);

        // <---- alice receives bob's message - this message is out of order
        final String aliceKeystoreAliasSelf = E2EEHandler.buildForSelf(aliceKeystoreAlias);
        assertTrue(E2EEHandler.isValidDefaultText(bobTransmissionText1));
        byte[] bobExtractedText_o3 = E2EEHandler.extractTransmissionText(bobTransmissionText1);
        byte[] bobPlainText_o3 = E2EEHandler.decrypt(context, aliceKeystoreAliasSelf,
                bobExtractedText_o3, null, null, true);
        assertArrayEquals(bobText1, bobPlainText_o3);

        // <---- alice receives bob's message
        assertTrue(E2EEHandler.isValidDefaultText(bobTransmissionText));
        byte[] bobExtractedText = E2EEHandler.extractTransmissionText(bobTransmissionText);
        byte[] bobPlainText = E2EEHandler.decrypt(context, aliceKeystoreAliasSelf, bobExtractedText,
                null, null, true);
        assertArrayEquals(bobText, bobPlainText);
    }


    @Test
    public void canDoubleRatchet() throws Throwable {
        String aliceAddress = "+237555555555";
        String bobAddress = "+237666666666";

        // Initial request
        String aliceKeystoreAlias = E2EEHandler.deriveKeystoreAlias(bobAddress, 0);
        Pair<String, byte[]> keystorePairAlice = E2EEHandler.buildForEncryptionRequest(context,
                bobAddress, null);
        byte[] aliceTransmissionKey = keystorePairAlice.second;

        // bob received alice's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(aliceTransmissionKey));
        String bobKeystoreAlias = E2EEHandler.deriveKeystoreAlias(aliceAddress, 0);
        byte[] aliceExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(aliceTransmissionKey);
        E2EEHandler.insertNewAgreementKeyDefault(context, aliceExtractedTransmissionKey, bobKeystoreAlias);

        // Agreement request
        Pair<String, byte[]> keystorePairBob = E2EEHandler
                .buildForEncryptionRequest(context, aliceAddress, null);
        byte[] bobTransmissionKey = keystorePairBob.second;

        // alice received bob's key
        assertTrue(E2EEHandler.isValidDefaultPublicKey(bobTransmissionKey));
        byte[] bobExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(bobTransmissionKey);
        E2EEHandler.insertNewAgreementKeyDefault(context, bobExtractedTransmissionKey, aliceKeystoreAlias);

        assertTrue(E2EEHandler.isAvailableInKeystore(aliceKeystoreAlias));
        assertTrue(E2EEHandler.isAvailableInKeystore(bobKeystoreAlias));

//        assertTrue(E2EEHandler.canCommunicateSecurely(context, aliceKeystoreAlias));
//        assertTrue(E2EEHandler.canCommunicateSecurely(context, bobKeystoreAlias));

        // ----> alice sends the message
         byte[] aliceText = CryptoHelpers.generateRandomBytes(130);
        byte[][] aliceCipherText = E2EEHandler.encrypt(context, aliceKeystoreAlias, aliceText, false);
        String aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText[0]);

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDefaultText(aliceTransmissionText));
        byte[] aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);
        byte[] alicePlainText = E2EEHandler.decrypt(context, bobKeystoreAlias, aliceExtractedText,
                null, null, false);
        assertArrayEquals(aliceText, alicePlainText);


        // <---- bob sends a message
         byte[] bobText = CryptoHelpers.generateRandomBytes(130);
        byte[][] bobCipherText = E2EEHandler.encrypt(context, bobKeystoreAlias, bobText, false);
        String bobTransmissionText = E2EEHandler.buildTransmissionText(bobCipherText[0]);

        // <----- bob receives the message again - as would be on mobile devices
        aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);
        alicePlainText = E2EEHandler.decrypt(context, bobKeystoreAlias, aliceExtractedText,
                aliceCipherText[1], null, false);
        assertArrayEquals(aliceText, alicePlainText);

        // <---- alice receives bob's message
        assertTrue(E2EEHandler.isValidDefaultText(bobTransmissionText));
        byte[] bobExtractedText = E2EEHandler.extractTransmissionText(bobTransmissionText);
        byte[] bobPlainText = E2EEHandler.decrypt(context, aliceKeystoreAlias, bobExtractedText,
                null, null, false);
        assertArrayEquals(bobText, bobPlainText);

        // <---- bob sends a message
        bobText = CryptoHelpers.generateRandomBytes(130);
        bobCipherText = E2EEHandler.encrypt(context, bobKeystoreAlias, bobText, false);
        bobTransmissionText = E2EEHandler.buildTransmissionText(bobCipherText[0]);

        // <---- then bob sends another
        byte[] bobText1 = CryptoHelpers.generateRandomBytes(130);
        byte[][] bobCipherText1 = E2EEHandler.encrypt(context, bobKeystoreAlias, bobText1, false);
        String bobTransmissionText1 = E2EEHandler.buildTransmissionText(bobCipherText1[0]);

        // ----> alice sends the message
        aliceText = CryptoHelpers.generateRandomBytes(130);
        aliceCipherText = E2EEHandler.encrypt(context, aliceKeystoreAlias, aliceText, false);
        aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText[0]);

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDefaultText(aliceTransmissionText));
        aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);
        alicePlainText = E2EEHandler.decrypt(context, bobKeystoreAlias, aliceExtractedText,
                null, null, false);
        assertArrayEquals(aliceText, alicePlainText);

        // <---- alice receives bob's message - this message is out of order
        assertTrue(E2EEHandler.isValidDefaultText(bobTransmissionText1));
        bobExtractedText = E2EEHandler.extractTransmissionText(bobTransmissionText1);
        bobPlainText = E2EEHandler.decrypt(context, aliceKeystoreAlias, bobExtractedText,
                null, null, false);
        assertArrayEquals(bobText1, bobPlainText);

        // <---- alice receives bob's message
        assertTrue(E2EEHandler.isValidDefaultText(bobTransmissionText));
        bobExtractedText = E2EEHandler.extractTransmissionText(bobTransmissionText);
        bobPlainText = E2EEHandler.decrypt(context, aliceKeystoreAlias, bobExtractedText,
                null, null, false);
        assertArrayEquals(bobText, bobPlainText);
    }
}
