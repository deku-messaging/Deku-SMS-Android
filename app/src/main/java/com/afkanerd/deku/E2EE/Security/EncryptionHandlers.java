package com.afkanerd.deku.E2EE.Security;

//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.OperatorCreationException;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import android.util.Base64;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class EncryptionHandlers {


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
        return EncryptionHandlers.dekuTextStartPrefix
                + text
                + EncryptionHandlers.dekuTextEndPrefix;
    }

    public static String removeEncryptedMessageWaterMark(String text) {
        int lastWaterMark = text.lastIndexOf(EncryptionHandlers.dekuTextEndPrefix);

        return text.substring(EncryptionHandlers.dekuTextStartPrefix.length(), lastWaterMark);
    }

    public static String removeKeyWaterMark(String text) {
        return text.replace(dekuHeaderStartPrefix, "")
                .replace(dekuHeaderEndPrefix, "");
    }

    public static boolean containersWaterMark(String text) {
        return text.indexOf(EncryptionHandlers.dekuTextStartPrefix) == 0 &&
                text.indexOf(EncryptionHandlers.dekuTextEndPrefix) ==
                        text.length() - EncryptionHandlers.dekuTextEndPrefix.length();
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

    public final static String dekuTextStartPrefix = "TDEKU{";
    public final static String dekuTextEndPrefix = "}UKEDT";

    public static String convertTextToDekuFormat(byte[] data) {
        return dekuTextStartPrefix
                + Base64.encodeToString(data, Base64.DEFAULT) +
                dekuTextEndPrefix;
    }

    public static byte[] convertPublicKeyToDekuFormat(byte[] data) {
        return Bytes.concat(dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8),
                data, dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8));
    }


}
