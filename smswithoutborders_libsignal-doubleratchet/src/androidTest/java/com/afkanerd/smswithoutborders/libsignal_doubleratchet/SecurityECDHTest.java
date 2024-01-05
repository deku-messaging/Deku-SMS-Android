package com.afkanerd.smswithoutborders.libsignal_doubleratchet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.util.Pair;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;

@RunWith(AndroidJUnit4.class)
public class SecurityECDHTest {

    String addressAlice = "+237612345678";
    String addressBob = "+237876543216";
    int session = 0;

    String keystoreAliasAlice = "bob_keystore_alias";
    String keystoreAliasBob = "alice_keystore_alias";
    Context context;

    public SecurityECDHTest() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void generateKeyPairTest() throws GeneralSecurityException, IOException, InterruptedException {
        String keystoreAlias = "sampleKeystoreAlias";
        SecurityECDH.generateKeyPair(keystoreAlias);
        assertTrue(KeystoreHelpers.isAvailableInKeystore(keystoreAlias));
        KeystoreHelpers.removeAllFromKeystore(context);
    }

    @Test
    public void canGetSameSharedSecretWithKnownParameters() throws GeneralSecurityException, IOException, InterruptedException {
        Pair<KeyPair, byte[]> alicePublicKey = SecurityECDH.generateKeyPair(keystoreAliasAlice);
        Pair<KeyPair, byte[]> bobPublicKey = SecurityECDH.generateKeyPair(keystoreAliasBob);

        byte[] aliceSharedSecret = SecurityECDH.generateSecretKey(alicePublicKey.first,
                bobPublicKey.first.getPublic());

        byte[] bobSharedSecret = SecurityECDH.generateSecretKey(bobPublicKey.first,
                alicePublicKey.first.getPublic());

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
        KeystoreHelpers.removeAllFromKeystore(context);
    }
}
