package com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityECDH;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

@RunWith(AndroidJUnit4.class)
public class RatchetsTest {

    Context context;
    PublicKey dhPublicKeyBob;
    KeyPair bobKeyPair;

    byte[] SK;

    public RatchetsTest() throws GeneralSecurityException, IOException, InterruptedException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // starting constants
        KeystoreHelpers.removeAllFromKeystore(context);

        SK = "115e74367d62f97538324202c0a3a4a2a77f6f79b597873875012a95152020f3".getBytes();

        String keystoreAliasBob = "bobsKeystoreAlias";
        bobKeyPair = SecurityECDH.generateKeyPair(keystoreAliasBob).first;
        dhPublicKeyBob = bobKeyPair.getPublic();
    }

    @Test
    public void completeRatchetTest() throws Throwable {
        Ratchets ratchetAlice = new Ratchets(), ratchetBob = new Ratchets();
        States stateAlice = new States(), stateBob = new States();

        String keystoreAliasAlice = "bob_session_0";
        ratchetAlice.ratchetInitAlice(keystoreAliasAlice, stateAlice, SK, dhPublicKeyBob);

        String keystoreAliasBob = "alice_session_0";
        ratchetBob.ratchetInitBob(stateBob, SK, bobKeyPair);

        // assertions
        States expectedStateAlice = new States(), expectedStateBob = new States();

        // alice params
        expectedStateAlice.DHs = SecurityECDH.generateKeyPair(keystoreAliasAlice).first;
        expectedStateAlice.DHr = dhPublicKeyBob;

        // bob params
        expectedStateBob.DHs = bobKeyPair;
        expectedStateBob.RK = SK;

        assertEquals(expectedStateAlice, stateAlice);
        assertEquals(expectedStateBob, stateBob);

        final byte[] plainText = CryptoHelpers.generateRandomBytes(130);
        final byte[] AD = CryptoHelpers.generateRandomBytes(16);
        Ratchets.EncryptPayload encryptPayloadAlice =
                ratchetAlice.ratchetEncrypt(stateAlice, plainText, AD);

        expectedStateAlice.Ns = 1;
        assertEquals(expectedStateAlice, stateAlice);

        Headers expectedHeadersAlice = new Headers(stateAlice.DHs, 0, 0);
        assertEquals(expectedHeadersAlice, encryptPayloadAlice.header);

        byte[] decryptedPlainText = ratchetBob.ratchetDecrypt(keystoreAliasBob, stateBob,
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

