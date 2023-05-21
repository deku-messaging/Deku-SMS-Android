package com.example.swob_deku.Models.Security;

//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.OperatorCreationException;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

public class SecurityHelpers {


    public final static String FIRST_HEADER = "--D.E.K.U.start---";
    public final static String END_HEADER = "--D.E.K.U.end---";

//    public final static String ENCRYPTED_WATERMARK = "\u007F";
//    public final static String ENCRYPTED_WATERMARK = "\u001B";

    public final static String ENCRYPTED_WATERMARK_START = "$d3$";
    public final static String ENCRYPTED_WATERMARK_END = ".d.$";

//    public static X509Certificate generateCertificate(KeyPair keyPair) throws NoSuchAlgorithmException, InvalidKeyException, IOException, CertificateException, OperatorCreationException, InvalidKeySpecException {
//        // Create self-signed certificate
//        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
//        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
//        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeySpec.getEncoded());
//
//        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
//                new X500Name("CN=DH Test Certificate"), // subject
//                BigInteger.valueOf(new SecureRandom().nextLong()), // serial number
//                new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L), // not before
//                new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L), // not after
//                new X500Name("CN=DH Test Certificate"), // issuer
//                subjectPublicKeyInfo);
//        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256withDSA");
//        signerBuilder.setProvider("BC");
//        ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
//        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
//    }

    public static byte[] txAgreementFormatter(byte[] agreementKey) {
        Log.d(SecurityHelpers.class.getName(), "Public key len: " + agreementKey.length);

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


    public static String waterMarkMessage(String text) {
        return SecurityHelpers.ENCRYPTED_WATERMARK_START
                + text
                + SecurityHelpers.ENCRYPTED_WATERMARK_END;
    }

    public static String removeWaterMarkMessage(String text) {
        int lastWaterMark = text.lastIndexOf(SecurityHelpers.ENCRYPTED_WATERMARK_END);

        return text.substring(SecurityHelpers.ENCRYPTED_WATERMARK_START.length(), lastWaterMark);
    }

    public static boolean containersWaterMark(String text) {
        return text.indexOf(SecurityHelpers.ENCRYPTED_WATERMARK_START) == 0 &&
                text.indexOf(SecurityHelpers.ENCRYPTED_WATERMARK_END) ==
                        text.length() - SecurityHelpers.ENCRYPTED_WATERMARK_END.length();
    }

    public static boolean isKeyExchange(String body) {
        return body.contains(FIRST_HEADER) && body.contains(END_HEADER);
    }
}
