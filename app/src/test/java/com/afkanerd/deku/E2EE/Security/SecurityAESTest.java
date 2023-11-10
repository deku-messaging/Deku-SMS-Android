package com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;

import com.afkanerd.deku.E2EE.Security.SecurityECDH;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class SecurityAESTest {

    @Test
    public void canEncryptDecryptAES256CBC() throws Throwable {
        byte[] plainText = SecurityHelpers.generateRandomBytes(140);
        byte[] sharedSecret = SecurityHelpers.generateRandomBytes(32);

        byte[] cipherText = SecurityAES.encryptAES256CBC(plainText, sharedSecret, null);
        byte[] plain = SecurityAES.decryptAES256CBC(cipherText, sharedSecret);

        assertArrayEquals(plain, plainText);
    }
}
