package com.example.swob_deku.Models.Security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
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

    public static X509Certificate generateCertificate(KeyPair keyPair) throws NoSuchAlgorithmException, InvalidKeyException, IOException, CertificateException, OperatorCreationException, InvalidKeySpecException {
        // Create self-signed certificate
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeySpec.getEncoded());

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                new X500Name("CN=DH Test Certificate"), // subject
                BigInteger.valueOf(new SecureRandom().nextLong()), // serial number
                new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L), // not before
                new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L), // not after
                new X500Name("CN=DH Test Certificate"), // issuer
                subjectPublicKeyInfo);
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256withDSA");
        signerBuilder.setProvider("BC");
        ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
    }

}
