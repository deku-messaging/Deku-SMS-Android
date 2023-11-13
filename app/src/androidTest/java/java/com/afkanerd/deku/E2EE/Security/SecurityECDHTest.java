package java.com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.afkanerd.deku.E2EE.Security.SecurityECDH;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

@RunWith(AndroidJUnit4.class)
public class SecurityECDHTest {

    String keystoreAlias = "TestAlias";
    @Test
    public void canGetSameSharedSecretWithKnownParameters() throws GeneralSecurityException, IOException, InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PublicKey alicePublicKey = SecurityECDH.generateKeyPair(context, keystoreAlias);
        PublicKey bobPublicKey = SecurityECDH.generateKeyPair(context, keystoreAlias);

        byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(keystoreAlias, bobPublicKey);
        byte[] bobSharedSecret = SecurityECDH.generateSecretKey(keystoreAlias, alicePublicKey);

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
    }

    @Test
    public void canGetSameSharedSecretWithUnknownParameters() throws GeneralSecurityException, IOException, InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PublicKey alicePublicKey = SecurityECDH.generateKeyPair(context, keystoreAlias);
        PublicKey bobPublicKey = SecurityECDH.generateKeyPairFromPublicKey(
                context, keystoreAlias, alicePublicKey).getPublic();

        byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(keystoreAlias, bobPublicKey);
        byte[] bobSharedSecret = SecurityECDH.generateSecretKey(keystoreAlias, alicePublicKey);

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
    }
}
