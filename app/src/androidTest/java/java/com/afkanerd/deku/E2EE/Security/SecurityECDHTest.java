package java.com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.google.i18n.phonenumbers.NumberParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;

@RunWith(AndroidJUnit4.class)
public class SecurityECDHTest {

    String addressAlice = "+237612345678";
    String addressBob = "+237876543216";
    int session = 0;

    String AliceKeystoreAlias;
    String BobKeystoreAlias;
    Context context;

    public SecurityECDHTest() throws NumberParseException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AliceKeystoreAlias = E2EEHandler.getKeyStoreAlias(addressAlice, session);
        BobKeystoreAlias = E2EEHandler.getKeyStoreAlias(addressBob, session);

        E2EEHandler.clearAll(context);
    }

    @Test
    public void canGetSameSharedSecretWithKnownParameters() throws GeneralSecurityException, IOException, InterruptedException {
        PublicKey alicePublicKey = SecurityECDH.generateKeyPair(context, BobKeystoreAlias);
        PublicKey bobPublicKey = SecurityECDH.generateKeyPair(context, AliceKeystoreAlias);

        byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(context, BobKeystoreAlias, bobPublicKey);
        byte[] bobSharedSecret = SecurityECDH.generateSecretKey(context, AliceKeystoreAlias, alicePublicKey);

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
    }

    @Test
    public void canGetSameSharedSecretWithUnknownParameters() throws GeneralSecurityException, IOException, InterruptedException {
        if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ) {
            PublicKey alicePublicKey = SecurityECDH.generateKeyPair(context, BobKeystoreAlias);
            PublicKey bobPublicKey = SecurityECDH.generateKeyPairFromPublicKey(
                    context, AliceKeystoreAlias, alicePublicKey).getPublic();

            byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(context, BobKeystoreAlias, bobPublicKey);
            byte[] bobSharedSecret = SecurityECDH.generateSecretKey(context, AliceKeystoreAlias, alicePublicKey);

            assertArrayEquals(aliceSharedSecret, bobSharedSecret);
        }
    }
}
