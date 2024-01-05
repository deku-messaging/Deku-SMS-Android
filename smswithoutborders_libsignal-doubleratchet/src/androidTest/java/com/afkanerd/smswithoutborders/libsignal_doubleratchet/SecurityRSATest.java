package com.afkanerd.smswithoutborders.libsignal_doubleratchet;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@RunWith(AndroidJUnit4.class)
public class SecurityRSATest {

    String keystoreAlias = "keystoreAlias";
    @Test
    public void testCanStoreAndEncrypt() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

        kpg.initialize(new KeyGenParameterSpec.Builder(keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build());

        KeyPair keyPair = kpg.generateKeyPair();

        SecretKey secretKey = SecurityAES.generateSecretKey(128);
        byte[] cipherText = SecurityRSA.encrypt(keyPair.getPublic(), secretKey.getEncoded());
        byte[] plainText = SecurityRSA.decrypt(keyPair.getPrivate(), cipherText);
        assertArrayEquals(secretKey.getEncoded(), plainText);
    }
}
