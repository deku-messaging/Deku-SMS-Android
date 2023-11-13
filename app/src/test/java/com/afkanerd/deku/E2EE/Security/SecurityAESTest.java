package com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SecurityAESTest {

    @Test
    public void canEncryptDecryptAES256CBC() throws Throwable {
        byte[] plainText = SecurityHandler.generateRandomBytes(140);
        byte[] sharedSecret = SecurityHandler.generateRandomBytes(32);

        byte[] cipherText = SecurityAES.encryptAES256CBC(plainText, sharedSecret, null);
        byte[] plain = SecurityAES.decryptAES256CBC(cipherText, sharedSecret);

        assertArrayEquals(plain, plainText);
    }
}
