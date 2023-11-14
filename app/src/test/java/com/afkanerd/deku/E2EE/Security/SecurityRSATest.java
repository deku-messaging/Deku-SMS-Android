package com.afkanerd.deku.E2EE.Security;

import static org.junit.Assert.assertEquals;

import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.junit.Test;
import org.spongycastle.jcajce.provider.symmetric.AES;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class SecurityRSATest {

    @Test
    public void testCanEncrypt() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
        keyGenerator.initialize(2048);
        KeyPair keyPair = keyGenerator.generateKeyPair();

        String data = "Hello world";
        byte[] cipherText = SecurityRSA.encrypt(keyPair.getPublic(), data.getBytes(StandardCharsets.UTF_8));
        byte[] plainText = SecurityRSA.decrypt(keyPair.getPrivate(), cipherText);
        assertEquals(data, new String(plainText));
    }
}
