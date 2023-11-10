package com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;

public class SecurityECDHTest {

    @Test
    public void canGetSameSharedSecretWithKnownParameters() throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH();
        KeyPair aliceKeyPair = securityECDH.getKeyPair();
        KeyPair bobKeyPair = securityECDH.getKeyPair();

        byte[] aliceSharedSecret = securityECDH.generateSecretKey(bobKeyPair.getPublic().getEncoded(),
                aliceKeyPair.getPrivate());

        byte[] bobSharedSecret = securityECDH.generateSecretKey(aliceKeyPair.getPublic().getEncoded(),
                bobKeyPair.getPrivate());

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
    }

    @Test
    public void canGetSameSharedSecretWithUnknownParameters() throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH();
        KeyPair aliceKeyPair = securityECDH.getKeyPair();
        KeyPair bobKeyPair =
                SecurityECDH.generateKeyPairFromPublicKey(aliceKeyPair.getPublic().getEncoded());

        byte[] aliceSharedSecret = securityECDH.generateSecretKey(bobKeyPair.getPublic().getEncoded(),
                aliceKeyPair.getPrivate());

        byte[] bobSharedSecret = securityECDH.generateSecretKey(aliceKeyPair.getPublic().getEncoded(),
                bobKeyPair.getPrivate());

        assertArrayEquals(aliceSharedSecret, bobSharedSecret);
    }
}
