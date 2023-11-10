package com.afkanerd.deku.E2EE.Security;

//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.OperatorCreationException;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class SecurityHelpers {


    public final static String FIRST_HEADER = "--D.E.K.U.start---";
    public final static String END_HEADER = "--D.E.K.U.end---";

//    public final static String ENCRYPTED_WATERMARK = "\u007F";
//    public final static String ENCRYPTED_WATERMARK = "\u001B";

    public final static String ENCRYPTED_WATERMARK_START = "$d3$";
    public final static String ENCRYPTED_WATERMARK_END = ".d.$";

    /**
     * Includes the headers required to identify that this is an agreement request.
     * @param agreementKey
     * @return
     */
    public static byte[] txAgreementFormatter(byte[] agreementKey) {
        byte[] firstHeader = FIRST_HEADER.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = END_HEADER.getBytes(StandardCharsets.US_ASCII);

        int SMS_CONSTANT = 130;

        byte[] startKey = new byte[agreementKey.length + firstHeader.length + endHeader.length];
        if(agreementKey.length + firstHeader.length + endHeader.length <= SMS_CONSTANT) {
            System.arraycopy(firstHeader, 0, startKey, 0, firstHeader.length);
            System.arraycopy(agreementKey, 0, startKey, firstHeader.length, agreementKey.length);
            System.arraycopy(endHeader, 0, startKey, agreementKey.length + firstHeader.length,
                    endHeader.length);
        }
        return startKey;
    }

    public static byte[] rxAgreementFormatter(byte[][] agreementKey) {

        byte[] firstHeader = FIRST_HEADER.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = END_HEADER.getBytes(StandardCharsets.US_ASCII);

        int dstLen = agreementKey[0].length - firstHeader.length;
        int dstLen1 = agreementKey[1].length - endHeader.length;

        byte[] agreementPubKey = new byte[dstLen + dstLen1];

        System.arraycopy(agreementKey[0], firstHeader.length, agreementPubKey, 0, dstLen);

        System.arraycopy(agreementKey[1], endHeader.length, agreementPubKey, dstLen, dstLen1);

        return agreementPubKey;
    }

    public static byte[] rxAgreementFormatter(byte[] agreementKey) {
        byte[] firstHeader = FIRST_HEADER.getBytes(StandardCharsets.US_ASCII);
        byte[] endHeader = END_HEADER.getBytes(StandardCharsets.US_ASCII);

        int keyLength = agreementKey.length - (firstHeader.length + endHeader.length);
        byte[] agreementPubKey = new byte[keyLength];

        System.arraycopy(agreementKey, firstHeader.length, agreementPubKey, 0, keyLength);

        return agreementPubKey;
    }


    public static String putEncryptedMessageWaterMark(String text) {
        return SecurityHelpers.ENCRYPTED_WATERMARK_START
                + text
                + SecurityHelpers.ENCRYPTED_WATERMARK_END;
    }

    public static String removeEncryptedMessageWaterMark(String text) {
        int lastWaterMark = text.lastIndexOf(SecurityHelpers.ENCRYPTED_WATERMARK_END);

        return text.substring(SecurityHelpers.ENCRYPTED_WATERMARK_START.length(), lastWaterMark);
    }

    public static String removeKeyWaterMark(String text) {
        return text.replace(FIRST_HEADER, "")
                .replace(END_HEADER, "");
    }

    public static boolean containersWaterMark(String text) {
        return text.indexOf(SecurityHelpers.ENCRYPTED_WATERMARK_START) == 0 &&
                text.indexOf(SecurityHelpers.ENCRYPTED_WATERMARK_END) ==
                        text.length() - SecurityHelpers.ENCRYPTED_WATERMARK_END.length();
    }

    public static boolean isKeyExchange(String body) {
        return body.contains(FIRST_HEADER) && body.contains(END_HEADER);
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
}
