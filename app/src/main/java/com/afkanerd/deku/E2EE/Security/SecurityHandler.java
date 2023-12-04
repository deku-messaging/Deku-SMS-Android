package com.afkanerd.deku.E2EE.Security;

//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.OperatorCreationException;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import android.os.Build;
import android.util.Base64;

import com.google.common.primitives.Bytes;
import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.mac.HmacParameters;
import com.google.crypto.tink.mac.MacConfig;
import com.google.crypto.tink.proto.HashType;
import com.google.crypto.tink.proto.HmacKeyFormat;
import com.google.crypto.tink.proto.HmacKeyFormatOrBuilder;
import com.google.crypto.tink.proto.HmacParams;
import com.google.crypto.tink.proto.HmacParamsOrBuilder;
import com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException;
import com.google.crypto.tink.subtle.Hkdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecurityHandler {


//    public final static String ENCRYPTED_WATERMARK = "\u007F";
//    public final static String ENCRYPTED_WATERMARK = "\u001B";


    /**
     * Includes the headers required to identify that this is an agreement request.
     *
     * @param agreementKey
     * @return
     */
    public static byte[] txAgreementFormatter(byte[] agreementKey) {
        byte[] firstHeader = dekuHeaderStartPrefix.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = dekuHeaderEndPrefix.getBytes(StandardCharsets.US_ASCII);

        int SMS_CONSTANT = 130;

        byte[] startKey = new byte[agreementKey.length + firstHeader.length + endHeader.length];
        if (agreementKey.length + firstHeader.length + endHeader.length <= SMS_CONSTANT) {
            System.arraycopy(firstHeader, 0, startKey, 0, firstHeader.length);
            System.arraycopy(agreementKey, 0, startKey, firstHeader.length, agreementKey.length);
            System.arraycopy(endHeader, 0, startKey, agreementKey.length + firstHeader.length,
                    endHeader.length);
        }
        return startKey;
    }

    public static byte[] rxAgreementFormatter(byte[][] agreementKey) {

        byte[] firstHeader = dekuHeaderStartPrefix.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = dekuHeaderEndPrefix.getBytes(StandardCharsets.US_ASCII);

        int dstLen = agreementKey[0].length - firstHeader.length;
        int dstLen1 = agreementKey[1].length - endHeader.length;

        byte[] agreementPubKey = new byte[dstLen + dstLen1];

        System.arraycopy(agreementKey[0], firstHeader.length, agreementPubKey, 0, dstLen);

        System.arraycopy(agreementKey[1], endHeader.length, agreementPubKey, dstLen, dstLen1);

        return agreementPubKey;
    }

    public static byte[] rxAgreementFormatter(byte[] agreementKey) {
        byte[] firstHeader = dekuHeaderStartPrefix.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = dekuHeaderEndPrefix.getBytes(StandardCharsets.US_ASCII);

        int keyLength = agreementKey.length - (firstHeader.length + endHeader.length);
        byte[] agreementPubKey = new byte[keyLength];

        System.arraycopy(agreementKey, firstHeader.length, agreementPubKey, 0, keyLength);

        return agreementPubKey;
    }


    public static String putEncryptedMessageWaterMark(String text) {
        return SecurityHandler.dekuTextStartPrefix
                + text
                + SecurityHandler.dekuTextEndPrefix;
    }

    public static String removeEncryptedMessageWaterMark(String text) {
        int lastWaterMark = text.lastIndexOf(SecurityHandler.dekuTextEndPrefix);

        return text.substring(SecurityHandler.dekuTextStartPrefix.length(), lastWaterMark);
    }

    public static String removeKeyWaterMark(String text) {
        return text.replace(dekuHeaderStartPrefix, "")
                .replace(dekuHeaderEndPrefix, "");
    }

    public static boolean containersWaterMark(String text) {
        return text.indexOf(SecurityHandler.dekuTextStartPrefix) == 0 &&
                text.indexOf(SecurityHandler.dekuTextEndPrefix) ==
                        text.length() - SecurityHandler.dekuTextEndPrefix.length();
    }

    public static boolean isKeyExchange(String body) {
        return body.contains(dekuHeaderStartPrefix) && body.contains(dekuHeaderEndPrefix);
    }


    public static PrivateKey bytesToPrivateKey(byte[] privateKeyBytes, String algorithm) throws
            NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm); // Replace "RSA" with your key algorithm
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        return keyFactory.generatePrivate(keySpec);
    }

    public static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new

                byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static PublicKey getPublicKeyFromKeyStore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.getCertificate(keystoreAlias).getPublicKey();
    }

    public final static String dekuHeaderStartPrefix = "HDEKU{";
    public final static String dekuHeaderEndPrefix = "}UKEDH";
    public final static String pemStartPrefix = "-----BEGIN PUBLIC KEY-----\n";
    public final static String pemEndPrefix = "\n-----END PUBLIC KEY-----";

    public final static String dekuTextStartPrefix = "TDEKU{";
    public final static String dekuTextEndPrefix = "}UKEDT";

    public static String convertTextToDekuFormat(byte[] data) {
        return dekuTextStartPrefix
                + Base64.encodeToString(data, Base64.DEFAULT) +
                dekuTextEndPrefix;
    }

    public static byte[] convertPublicKeyToDekuFormat(byte[] data) {
//        byte[] encodedString = new byte[data.length +
//                dekuHeaderStartPrefix.length() +
//                dekuHeaderEndPrefix.length()];

//        System.arraycopy(dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8), 0,
//                encodedString, 0, dekuHeaderStartPrefix.length());
//
//        System.arraycopy(data, 0, encodedString, dekuHeaderStartPrefix.length(), data.length);
//
//        System.arraycopy(dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8), 0,
//                encodedString, dekuHeaderStartPrefix.length() + data.length,
//                dekuHeaderEndPrefix.length());

        return Bytes.concat(dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8),
                data, dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8));
    }

    public static String convertPublicKeyToPEMFormat(byte[] publicKey) {
        return pemStartPrefix
                + Base64.encodeToString(publicKey, Base64.DEFAULT) +
                pemEndPrefix;
    }

    protected static KeyPair getKeyPairFromKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(keystoreAlias, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            PublicKey publicKey = keyStore.getCertificate(keystoreAlias).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        }
        return null;
    }

    public static byte[][] HKDF(String algo, byte[] ikm, byte[] salt, byte[] info, int len, int num) throws GeneralSecurityException {
        if (num < 1)
            num = 1;
        byte[] output = Hkdf.computeHkdf(algo, ikm, salt, info, len * num);
        byte[][] outputs = new byte[num][len];
        for (int i = 0; i < num; ++i) {
            System.arraycopy(output, i * len, outputs[i], 0, len);
        }
        return outputs;
    }

    public static Mac HMAC(byte[] data) throws InvalidProtocolBufferException, GeneralSecurityException {
        String algorithm = "HmacSHA256";
        Mac hmacSHA256 = Mac.getInstance(algorithm);
        SecretKey key = new SecretKeySpec(data, algorithm);
        hmacSHA256.init(key);
        return hmacSHA256;
    }
}
