package com.afkanerd.smswithoutborders.libsignal_doubleratchet;

import android.security.keystore.KeyProperties;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class SecurityRSA {

    public static MGF1ParameterSpec defaultEncryptionDigest = MGF1ParameterSpec.SHA256;
    public static MGF1ParameterSpec defaultDecryptionDigest = MGF1ParameterSpec.SHA1;

    public static OAEPParameterSpec encryptionDigestParam =
            new OAEPParameterSpec("SHA-256", "MGF1", defaultEncryptionDigest,
                    PSource.PSpecified.DEFAULT);
    public static OAEPParameterSpec decryptionDigestParam =
            new OAEPParameterSpec("SHA-256", "MGF1", defaultDecryptionDigest,
                    PSource.PSpecified.DEFAULT);

    public static byte[] decrypt(PrivateKey privateKey, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        Cipher cipher = Cipher.getInstance("RSA/ECB/" + KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
//        cipher.init(Cipher.DECRYPT_MODE, privateKey, decryptionDigestParam);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    public static byte[] encrypt(PublicKey publicKey, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/" + KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey, encryptionDigestParam);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }
}
