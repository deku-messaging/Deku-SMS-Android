package java.com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.google.i18n.phonenumbers.NumberParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class ConversationsThreadsEncryptionTest {

    Context context;
    String address = "+237612345678";
    int sessionNumber = 0;
    String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=";

    public ConversationsThreadsEncryptionTest() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        E2EEHandler.clearAll(context);
    }

    @Test
    public void getKeyStoreAliasTest() throws NumberParseException {
        String outputKeystoreAlias = E2EEHandler.getKeyStoreAlias(address, sessionNumber);
        assertEquals(keystoreAlias, outputKeystoreAlias);
    }

    @Test
    public void testCanCreateAndRemoveKeyPair() throws GeneralSecurityException, IOException, InterruptedException {
        E2EEHandler.createNewKeyPair(context, keystoreAlias);
        assertTrue(E2EEHandler.isAvailableInKeystore(keystoreAlias));

        int numberRemoved = E2EEHandler.removeFromKeystore(context, keystoreAlias);
        assertFalse(E2EEHandler.isAvailableInKeystore(keystoreAlias));

        if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S )
            assertEquals(1, numberRemoved);
        E2EEHandler.clearAll(context);
    }

    @Test
    public void testSize() throws GeneralSecurityException, IOException, InterruptedException {
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        Log.d(getClass().getName(), "Length: " + publicKey.getEncoded().length);
        Log.d(getClass().getName(), "Length Encoded: " +
                Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT).length());
        int length = publicKey.getEncoded().length;

        assertEquals(91, length);
        E2EEHandler.clearAll(context);
    }

    @Test
    public void testIsValidAgreementPubKey() throws GeneralSecurityException, IOException, InterruptedException {
        PublicKey publicKey = E2EEHandler.createNewKeyPair(context, keystoreAlias);
        String dekuPublicKey = E2EEHandler.buildDekuPublicKey(publicKey.getEncoded());
        assertTrue(E2EEHandler.isValidDekuPublicKey(dekuPublicKey));

        SecretKey secretKey = SecurityAES.generateSecretKey(91);
        String invalidPublicKey = E2EEHandler.buildDekuPublicKey(secretKey.getEncoded());
        assertFalse(E2EEHandler.isValidDekuPublicKey(invalidPublicKey));
        E2EEHandler.clearAll(context);
    }

    @Test
    public void testBuildForEncryptionRequest() throws GeneralSecurityException, NumberParseException, IOException, InterruptedException {
        String transmissionRequest = E2EEHandler.buildForEncryptionRequest(context, address);
        Log.d(getClass().getName(), "Transmission request size: " +
                transmissionRequest.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(E2EEHandler.isValidDekuPublicKey(transmissionRequest));
    }
    @Test
    public void canBeTransmittedAsData() throws GeneralSecurityException, NumberParseException, IOException, InterruptedException {
        String transmissionRequest = E2EEHandler.buildForEncryptionRequest(context, address);
        assertTrue(transmissionRequest.getBytes(StandardCharsets.UTF_8).length < 140);
    }

    @Test
    public void canE2EE() throws NumberParseException, GeneralSecurityException, IOException, InterruptedException {
        String aliceAddress = "+237612345678";
        String bobAddress = "+237612345670";

        String aliceKeystoreAlias = E2EEHandler.getKeyStoreAlias(aliceAddress, 0);
        String bobKeystoreAlias = E2EEHandler.getKeyStoreAlias(bobAddress, 0);

        PublicKey alicePublicKey = E2EEHandler.createNewKeyPair(context, bobKeystoreAlias);
        Log.d(getClass().getName(), "Alice public key: " +
                Base64.encodeToString(alicePublicKey.getEncoded(),Base64.DEFAULT));

        PublicKey bobPublicKey = E2EEHandler.createNewKeyPair(context, aliceKeystoreAlias);
        Log.d(getClass().getName(), "Bob public key: " +
                Base64.encodeToString(bobPublicKey.getEncoded(),Base64.DEFAULT));

        byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(context, bobKeystoreAlias, bobPublicKey);
        byte[] bobSharedSecret = SecurityECDH.generateSecretKey(context, aliceKeystoreAlias, alicePublicKey);
        assertArrayEquals(aliceSharedSecret, bobSharedSecret);


        String aliceTransmissionKey = E2EEHandler.buildDekuPublicKey(alicePublicKey.getEncoded());
        String bobTransmissionKey = E2EEHandler.buildDekuPublicKey(bobPublicKey.getEncoded());

        // sends key over data channel

        // bob received alice's key
        assertTrue(E2EEHandler.isValidDekuPublicKey(aliceTransmissionKey));
        byte[] aliceExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(aliceTransmissionKey);
        assertArrayEquals(alicePublicKey.getEncoded(), aliceExtractedTransmissionKey);
        E2EEHandler.insertNewPeerPublicKey(context, aliceExtractedTransmissionKey, aliceKeystoreAlias);

        // alice received bob's key
        assertTrue(E2EEHandler.isValidDekuPublicKey(bobTransmissionKey));
        byte[] bobExtractedTransmissionKey = E2EEHandler.extractTransmissionKey(bobTransmissionKey);
        assertArrayEquals(bobPublicKey.getEncoded(), bobExtractedTransmissionKey);
        E2EEHandler.insertNewPeerPublicKey(context, bobExtractedTransmissionKey, bobKeystoreAlias);

        String aliceText = "Hello world!";
        byte[] aliceCipherText = E2EEHandler.encryptText(context, bobKeystoreAlias, aliceText);
        String aliceTransmissionText = E2EEHandler.buildTransmissionText(aliceCipherText);
        // ----> alice sends the message

        // <----- bob receives the message
        assertTrue(E2EEHandler.isValidDekuText(aliceTransmissionText));
        byte[] aliceExtractedText = E2EEHandler.extractTransmissionText(aliceTransmissionText);
        assertArrayEquals(aliceCipherText, aliceExtractedText);

        byte[] alicePlainText = E2EEHandler.decryptText(context, aliceKeystoreAlias, aliceExtractedText);
        assertEquals(aliceText, new String(alicePlainText));
    }

}
