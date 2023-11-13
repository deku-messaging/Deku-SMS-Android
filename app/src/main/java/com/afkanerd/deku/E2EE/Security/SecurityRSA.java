package com.afkanerd.deku.E2EE.Security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SecurityRSA {
    public static byte[] decrypt(PrivateKey privateKey, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    public static byte[] encrypt(PublicKey publicKey, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
}
