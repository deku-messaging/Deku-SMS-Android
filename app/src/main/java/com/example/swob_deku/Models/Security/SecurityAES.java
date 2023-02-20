package com.example.swob_deku.Models.Security;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.security.AlgorithmConstraints;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class SecurityAES {

    public static MGF1ParameterSpec defaultEncryptionDigest = MGF1ParameterSpec.SHA256;
    public static MGF1ParameterSpec defaultDecryptionDigest = MGF1ParameterSpec.SHA1;

    public static final String DEFAULT_AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    public SecurityAES(){
    }

    public byte[] encrypt(byte[] iv, byte[] input, byte[] sharedKey) throws Throwable {
        byte[] ciphertext = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(sharedKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(DEFAULT_AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            ciphertext = cipher.doFinal(input);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
        return ciphertext;
    }

    public byte[] decrypt(byte[] iv, byte[] input, byte[] sharedKey) throws Throwable {
        byte[] decryptedText = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(sharedKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(DEFAULT_AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            decryptedText = cipher.doFinal(input);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
        return decryptedText;
    }
}
