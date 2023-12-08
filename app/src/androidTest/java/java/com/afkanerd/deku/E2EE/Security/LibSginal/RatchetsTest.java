package java.com.afkanerd.deku.E2EE.Security.LibSginal;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.E2EE.Security.LibSignal.Headers;
import com.afkanerd.deku.E2EE.Security.LibSignal.Protocols;
import com.afkanerd.deku.E2EE.Security.LibSignal.Ratchets;
import com.afkanerd.deku.E2EE.Security.LibSignal.States;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHandler;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

@RunWith(AndroidJUnit4.class)
public class RatchetsTest {

    Context context;
    PublicKey dhPublicKeyBob;

    byte[] SK;
    String bobKeyPairForAliceKeystoreAlias = "alice_session_0";

    public RatchetsTest() throws GeneralSecurityException, IOException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // starting constants
        E2EEHandler.clearAll(context);
//        SK = SecurityHandler.generateRandomBytes(32);
        SK = "115e74367d62f97538324202c0a3a4a2a77f6f79b597873875012a95152020f3".getBytes();

        dhPublicKeyBob = E2EEHandler.createNewKeyPair(context, bobKeyPairForAliceKeystoreAlias);
    }

    @Test
    public void completeRatchetTest() throws Throwable {
        Ratchets ratchetAlice = new Ratchets(), ratchetBob = new Ratchets();
        States stateAlice = new States(), stateBob = new States();

        String aliceKeyPairForBobKeystoreAlias = "bob_session_0";
        E2EEHandler.createNewKeyPair(context, aliceKeyPairForBobKeystoreAlias);
        KeyPair aliceKeyPair = E2EEHandler.getKeyPairFromKeystore(context,
                aliceKeyPairForBobKeystoreAlias);

        ratchetAlice.ratchetInitAlice(context, aliceKeyPairForBobKeystoreAlias,
                stateAlice, SK, dhPublicKeyBob);

        KeyPair bobKeyPair = E2EEHandler.getKeyPairFromKeystore(context,
                bobKeyPairForAliceKeystoreAlias);
        ratchetBob.ratchetInitBob(stateBob, SK, bobKeyPair);

        // assertions
        States expectedStateAlice = new States(), expectedStateBob = new States();

        // alice params
        expectedStateAlice.DHs = aliceKeyPair;
        expectedStateAlice.DHr = dhPublicKeyBob;

        // bob params
        expectedStateBob.DHs = bobKeyPair;
        expectedStateBob.RK = SK;

        assertEquals(expectedStateAlice, stateAlice);
        assertEquals(expectedStateBob, stateBob);

        final byte[] plainText = SecurityHandler.generateRandomBytes(130);
        final byte[] AD = SecurityHandler.generateRandomBytes(16);
        Ratchets.EncryptPayload encryptPayloadAlice =
                ratchetAlice.ratchetEncrypt(stateAlice, plainText, AD);

        expectedStateAlice.Ns = 1;
        assertEquals(expectedStateAlice, stateAlice);

        Headers expectedHeadersAlice = new Headers(stateAlice.DHs, 0, 0);
        assertEquals(expectedHeadersAlice, encryptPayloadAlice.header);

        Log.d(Ratchets.class.getName(), "Payload size: " +
                Base64.encodeBase64String(encryptPayloadAlice.cipherText).length());

        byte[] decryptedPlainText = ratchetBob.ratchetDecrypt(
                context, bobKeyPairForAliceKeystoreAlias, stateBob,
                encryptPayloadAlice.header, encryptPayloadAlice.cipherText,
                Protocols.CONCAT(AD, encryptPayloadAlice.header));
        expectedStateBob.PN = 0;
        expectedStateBob.Ns = 0;
        expectedStateBob.Nr = 1;
        expectedStateBob.DHr = stateAlice.DHs.getPublic();
        assertArrayEquals(expectedStateBob.DHr.getEncoded(),
                stateBob.DHr.getEncoded());
        assertEquals(expectedStateBob, stateBob);

        assertArrayEquals(plainText, decryptedPlainText);
    }
}

