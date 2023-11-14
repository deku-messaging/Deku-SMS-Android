package com.afkanerd.deku.E2EE.Security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
        return aesCipher.doFinal(data);
    }

    public static byte[] decryptAESGCM(byte[] data, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
        return aesCipher.doFinal(data);
    }

    public static byte[] encryptAES256CBC(byte[] input, byte[] secretKey, byte[] iv) throws Throwable {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");

            Cipher cipher = Cipher.getInstance(DEFAULT_AES_ALGORITHM);
            if(iv != null) {
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            }
            byte[] ciphertext = cipher.doFinal(input);

            byte[] cipherTextIv = new byte[16 + ciphertext.length];
            System.arraycopy(cipher.getIV(), 0,  cipherTextIv, 0, 16);
            System.arraycopy(ciphertext, 0,  cipherTextIv, 16, ciphertext.length);

            return cipherTextIv;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
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
