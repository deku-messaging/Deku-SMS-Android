package java.com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.i18n.phonenumbers.NumberParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

@RunWith(AndroidJUnit4.class)
public class ConversationsThreadsEncryptionTest {

    @Test
    public void getKeyStoreAliasTest() throws NumberParseException {
        String address = "+237612345678";
        int sessionNumber = 0;
        String outputKeystoreAlias = E2EEHandler.getKeyStoreAlias(address, sessionNumber);

        String expectedKeystoreAlias = "MjM3NjEyMzQ1Njc4XzA=";
        assertEquals(expectedKeystoreAlias, outputKeystoreAlias);
    }

    @Test
    public void testCanCreateAndRemoveKeyPair() throws GeneralSecurityException, IOException, InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String keystoreAlias = "MjM3NjEyMzQ1Njc4XzA=";

        E2EEHandler.createNewKeyPair(context, keystoreAlias);
        assertTrue(E2EEHandler.isAvailableInKeystore(keystoreAlias));

        int numberRemoved = E2EEHandler.removeFromKeystore(context, keystoreAlias);
        assertFalse(E2EEHandler.isAvailableInKeystore(keystoreAlias));
        assertEquals(1, numberRemoved);
    }
}
