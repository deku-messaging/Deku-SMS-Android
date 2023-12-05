package com.afkanerd.deku.E2EE.Security.LibSignal;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHandler;
import com.google.common.primitives.Bytes;
import com.google.crypto.tink.proto.Hmac;
import com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException;

import org.spongycastle.jcajce.provider.digest.SHA256;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;

public class Protocols {

    // TODO
    public int MAX_SKIP = 0;

    final static int HKDF_LEN = 32;
    final static int HKDF_NUM_KEYS = 2;
    final static int SHA256_DIGEST_LEN = 32;

    final static String ALGO = "HMACSHA512";

    public static KeyPair GENERATE_DH(Context context, String keystoreAlias) throws GeneralSecurityException, IOException, InterruptedException {
        E2EEHandler.createNewKeyPair(context, keystoreAlias);
        return E2EEHandler.getKeyPairFromKeystore(context, keystoreAlias);
    }

    public static byte[] DH(KeyPair dhPair, PublicKey publicKey) throws GeneralSecurityException, IOException, InterruptedException {
        return SecurityECDH.generateSecretKey(dhPair, publicKey);
    }

    public static byte[][] KDF_RK(byte[] rk, byte[] dhOut) throws GeneralSecurityException {
        byte[] info = "KDF_RK".getBytes();
        return SecurityHandler.HKDF(ALGO, dhOut, rk, info, HKDF_LEN, HKDF_NUM_KEYS);
    }

    public static byte[][] KDF_CK(byte[] ck) throws GeneralSecurityException, InvalidProtocolBufferException {
        Mac mac = SecurityHandler.HMAC(ck);
        ck = mac.doFinal(new byte[]{0x01});
        byte[] mk = mac.doFinal(new byte[]{0x02});
        return new byte[][]{ck, mk};
    }

    public static byte[] ENCRYPT(byte[] mk, byte[] plainText, byte[] associated_data) throws Throwable {
        byte[] hkdfOutput = getCipherMacParameters(mk);
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
        cipherText = verifyCipherText(mk, cipherText, associated_data);

        byte[] hkdfOutput = getCipherMacParameters(mk);
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

    private static byte[] getCipherMacParameters(byte[] mk) throws GeneralSecurityException {
        int hashLen = 80;
        byte[] info = "ENCRYPT".getBytes();
        byte[] salt = new byte[hashLen];
        Arrays.fill(salt, (byte) 0);

        return SecurityHandler.HKDF(ALGO, mk, salt, info, hashLen, 1)[0];
    }

    private static Mac buildVerificationHash(byte[] authKey, byte[] AD, byte[] cipherText) throws GeneralSecurityException, InvalidProtocolBufferException {
        Mac mac = SecurityHandler.HMAC(authKey);
        mac.update(Bytes.concat(AD, cipherText));
        return mac;
    }

    private static byte[] verifyCipherText(byte[] mk, byte[] cipherText, byte[] AD) throws Exception {
        byte[] hkdfOutput = getCipherMacParameters(mk);
        byte[] key = new byte[32];
        byte[] authenticationKey = new byte[32];
        byte[] iv = new byte[16];

        System.arraycopy(hkdfOutput, 32, authenticationKey, 0, 32);

        byte[] macValue = new byte[SHA256_DIGEST_LEN];
        System.arraycopy(cipherText, cipherText.length - SHA256_DIGEST_LEN,
                macValue, 0, SHA256_DIGEST_LEN);

        byte[] extractedCipherText = new byte[cipherText.length - SHA256_DIGEST_LEN];
        System.arraycopy(cipherText, 0, extractedCipherText,
                0, extractedCipherText.length);

        byte[] reconstructedMac =
                buildVerificationHash(authenticationKey, AD, extractedCipherText)
                        .doFinal();

        if(Arrays.equals(macValue, reconstructedMac)) {
            return extractedCipherText;
        }
        throw new Exception("Cipher signature verification failed");
    }
}

