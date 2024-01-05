package com.afkanerd.smswithoutborders.libsignal_doubleratchet;

import com.google.common.primitives.Bytes;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityAES {

    public static final String DEFAULT_AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    public static final String ALGORITHM = "AES";


    public static SecretKey generateSecretKey(int size) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(size); // Adjust key size as needed
        return keyGenerator.generateKey();
    }

    public static byte[] encryptAESGCM(byte[] data, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] cipherText = aesCipher.doFinal(data);

        final byte[] IV = aesCipher.getIV();
        byte[] cipherTextIv = new byte[IV.length + cipherText.length];
        System.arraycopy(IV, 0,  cipherTextIv, 0, IV.length);
        System.arraycopy(cipherText, 0,  cipherTextIv, IV.length, cipherText.length);
        return cipherTextIv;
    }

    public static byte[] decryptAESGCM(byte[] data, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, iv.length);

        byte[] _data = new byte[data.length - iv.length];
        System.arraycopy(data, iv.length, _data, 0, _data.length);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128,iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        return aesCipher.doFinal(_data);
    }

    public static byte[] encryptAES256CBC(byte[] input, byte[] secretKey, byte[] iv) throws Throwable {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");

        Cipher cipher = Cipher.getInstance(DEFAULT_AES_ALGORITHM);
        if(iv != null) {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        }
        byte[] ciphertext = cipher.doFinal(input);
        return Bytes.concat(cipher.getIV(), ciphertext);
    }

    public static byte[] decryptAES256CBC(byte[] input, byte[] sharedKey) throws Throwable {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(sharedKey, ALGORITHM);

            byte[] iv = new byte[16];
            System.arraycopy(input, 0, iv, 0, 16);

            byte[] content = new byte[input.length - 16];
            System.arraycopy(input, 16, content, 0, content.length);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(DEFAULT_AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(content);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
    }
}
