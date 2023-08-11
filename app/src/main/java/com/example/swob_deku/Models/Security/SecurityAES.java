package com.example.swob_deku.Models.Security;

import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityAES {

    public static MGF1ParameterSpec defaultEncryptionDigest = MGF1ParameterSpec.SHA256;
    public static MGF1ParameterSpec defaultDecryptionDigest = MGF1ParameterSpec.SHA1;

    public static final String DEFAULT_AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    public SecurityAES(){
    }

    public static byte[] encrypt_256_cbc(byte[] input, byte[] secretKey, byte[] iv) throws Throwable {
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

    public static byte[] decrypt_256_cbc(byte[] input, byte[] sharedKey) throws Throwable {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(sharedKey, "AES");

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
