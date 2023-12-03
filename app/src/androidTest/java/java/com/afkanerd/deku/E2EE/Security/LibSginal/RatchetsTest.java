package java.com.afkanerd.deku.E2EE.Security.LibSginal;


import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.E2EE.Security.LibSignal.Protocols;
import com.afkanerd.deku.E2EE.Security.LibSignal.Ratchets;
import com.afkanerd.deku.E2EE.Security.LibSignal.States;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHandler;

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
        // starting constants
        SK = SecurityHandler.generateRandomBytes(32);

        dhPublicKeyBob = E2EEHandler.createNewKeyPair(context, bobKeyPairForAliceKeystoreAlias);
    }

    @Test
    public void completeRatchetTest() throws GeneralSecurityException, IOException, InterruptedException {
        Ratchets ratchetAlice = new Ratchets(), ratchetBob = new Ratchets();
        States stateAlice = new States(), stateBob = new States();

        String aliceKeyPairForBobKeystoreAlias = "bob_session_0";
        E2EEHandler.createNewKeyPair(context, aliceKeyPairForBobKeystoreAlias);
        KeyPair aliceKeyPair = SecurityHandler.getKeyPairFromKeystore(aliceKeyPairForBobKeystoreAlias);

        ratchetAlice.ratchetInitAlice(stateAlice, SK, dhPublicKeyBob);

        KeyPair bobKeyPair = SecurityHandler.getKeyPairFromKeystore(bobKeyPairForAliceKeystoreAlias);
        ratchetBob.ratchetInitBob(stateBob, SK, bobKeyPair);


        // assertions
        States expectedStateAlice = new States(), expectedStateBob = new States();

        // alice params
        expectedStateAlice.DHs = aliceKeyPair;
        expectedStateAlice.DHr = dhPublicKeyBob;
        byte[][] kdfRkOutput = Protocols.KDF_RK(SK,
                Protocols.DH(expectedStateAlice.DHs, expectedStateAlice.DHr));
        expectedStateAlice.RK = kdfRkOutput[0];
        expectedStateAlice.CKs = kdfRkOutput[1];

        // bob params
        expectedStateBob.DHs = bobKeyPair;
        expectedStateBob.RK = SK;

        assertEquals(expectedStateAlice, stateAlice);
        assertEquals(expectedStateBob, stateBob);
    }
}
