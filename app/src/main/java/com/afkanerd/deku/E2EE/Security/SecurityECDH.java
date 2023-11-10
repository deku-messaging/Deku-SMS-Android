package com.afkanerd.deku.E2EE.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

//import org.bouncycastle.operator.OperatorCreationException;

import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.crypto.KeyAgreement;

public class SecurityECDH {
    public final static String DEFAULT_ALGORITHM = "ECDH";

    public final int DEFAULT_KEY_SIZE = 256;
    private static final String PROVIDER = "SC";

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }
    public byte[] generateSecretKey(byte[] peerPublicKey, PrivateKey privateKey) throws GeneralSecurityException, IOException {
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(peerPublicKey);

        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);

        KeyAgreement keyAgree  = KeyAgreement.getInstance(DEFAULT_ALGORITHM);

        keyAgree.init(privateKey);
        keyAgree.doPhase(publicKey, true);

        return keyAgree.generateSecret();
    }

    public KeyPair getKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM, PROVIDER);
        keyPairGenerator.initialize(DEFAULT_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Use this if parameters used by other party is not known.
     * Would extract the parameters from their public key.
     * @param publicKeyEnc
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static KeyPair generateKeyPairFromPublicKey(byte[] publicKeyEnc) throws GeneralSecurityException, IOException {
        KeyFactory bobKeyFac = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyEnc);

        PublicKey publicKey = bobKeyFac.generatePublic(x509KeySpec);

        ECParameterSpec dhParameterSpec = ((BCECPublicKey)publicKey).getParams();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        keyPairGenerator.initialize(dhParameterSpec);

        return keyPairGenerator.generateKeyPair();
    }

}
