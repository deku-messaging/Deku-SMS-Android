package com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;

public class SecurityAESRandomTest {

    @Test
    public void canEncryptDecryptAES256CBC() throws Throwable {
        byte[] plainText = EncryptionHandlers.generateRandomBytes(140);
        byte[] sharedSecret = EncryptionHandlers.generateRandomBytes(32);

        byte[] cipherText = SecurityAES.encryptAES256CBC(plainText, sharedSecret, null);
        byte[] plain = SecurityAES.decryptAES256CBC(cipherText, sharedSecret);

        assertArrayEquals(plain, plainText);
    }

    @Test
    public void canEncryptDecryptAESGCM() throws Throwable {
        byte[] plainText = EncryptionHandlers.generateRandomBytes(140);
        byte[] sharedSecret = EncryptionHandlers.generateRandomBytes(32);

        byte[] cipherText = SecurityAES.encryptAESGCM(plainText,
                new SecretKeySpec(sharedSecret, "AES"));

        byte[] plain = SecurityAES.decryptAESGCM(cipherText,
                new SecretKeySpec(sharedSecret, "AES"));

        assertArrayEquals(plain, plainText);
    }
}
