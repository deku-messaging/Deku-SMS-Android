package com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal;

import static com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers.buildVerificationHash;
import static com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers.getCipherMacParameters;
import static com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers.verifyCipherText;

import android.content.Context;

import com.afkanerd.smswithoutborders.libsignal_doubleratchet.CryptoHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityAES;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityECDH;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.Mac;

/**
 * This implementations are based on the signal protocols specifications.
 *
 * This are based on the recommended algorithms and parameters for the encryption
 * and decryption.
 *
 * The goal for this would be to transform it into library which can be used across
 * other SMS projects.
 *
 * <a href="https://signal.org/docs/specifications/doubleratchet/">...</a>
 */
public class Protocols {

    // TODO
    public int MAX_SKIP = 0;

    final static int HKDF_LEN = 32;
    final static int HKDF_NUM_KEYS = 2;
    final static String ALGO = "HMACSHA512";

    public static KeyPair GENERATE_DH(String keystoreAlias) throws GeneralSecurityException, IOException, InterruptedException {
        return SecurityECDH.generateKeyPair(keystoreAlias).first;
    }

    public static byte[] DH(KeyPair dhPair, PublicKey publicKey) throws GeneralSecurityException, IOException, InterruptedException {
        return SecurityECDH.generateSecretKey(dhPair, publicKey);
    }

    public static byte[][] KDF_RK(byte[] rk, byte[] dhOut) throws GeneralSecurityException {
        byte[] info = "KDF_RK".getBytes();
        return CryptoHelpers.HKDF(ALGO, dhOut, rk, info, HKDF_LEN, HKDF_NUM_KEYS);
    }

    public static byte[][] KDF_CK(byte[] ck) throws GeneralSecurityException {
        Mac mac = CryptoHelpers.HMAC(ck);
        ck = mac.doFinal(new byte[]{0x01});
        byte[] mk = mac.doFinal(new byte[]{0x02});
        return new byte[][]{ck, mk};
    }

    public static byte[] ENCRYPT(byte[] mk, byte[] plainText, byte[] associated_data) throws Throwable {
        byte[] hkdfOutput = getCipherMacParameters(ALGO, mk);
        byte[] key = new byte[32];
        byte[] authenticationKey = new byte[32];
        byte[] iv = new byte[16];

        System.arraycopy(hkdfOutput, 0, key, 0, 32);
        System.arraycopy(hkdfOutput, 32, authenticationKey, 0, 32);
        System.arraycopy(hkdfOutput, 64, iv, 0, 16);

        byte[] cipherText = SecurityAES.encryptAES256CBC(plainText, key, iv);
        byte[] mac = buildVerificationHash(authenticationKey, associated_data, cipherText).doFinal();

        return Bytes.concat(cipherText, mac) ;
    }

    public static byte[] DECRYPT(byte[] mk, byte[] cipherText, byte[] associated_data) throws Throwable {
        cipherText = verifyCipherText(ALGO, mk, cipherText, associated_data);

        byte[] hkdfOutput = getCipherMacParameters(ALGO, mk);
        byte[] key = new byte[32];
        System.arraycopy(hkdfOutput, 0, key, 0, 32);

        return SecurityAES.decryptAES256CBC(cipherText, key);
    }

    public static Headers HEADER(KeyPair dhPair, int PN, int N) {
        return new Headers(dhPair, PN, N);
    }

    public static byte[] CONCAT(byte[] AD, Headers headers) {
        return Bytes.concat(AD, headers.getSerialized());
    }

}

