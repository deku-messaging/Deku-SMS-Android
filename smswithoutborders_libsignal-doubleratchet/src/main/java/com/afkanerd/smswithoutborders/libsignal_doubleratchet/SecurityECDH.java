package com.afkanerd.smswithoutborders.libsignal_doubleratchet;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;


import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;

public class SecurityECDH {
    public final static String DEFAULT_ALGORITHM = "ECDH";

    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static PublicKey buildPublicKey(byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Assuming the key is in X.509 format (common for RSA keys)
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(SecurityECDH.DEFAULT_ALGORITHM);

        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey buildPrivateKey(byte[] privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
        return keyFactory.generatePrivate(keySpec);
    }


    public static PrivateKey getPrivateKeyFromKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return (PrivateKey) keyStore.getKey(keystoreAlias, null);
    }

    public static byte[] generateSecretKey(KeyPair keyPair, PublicKey publicKey)
            throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(DEFAULT_ALGORITHM);
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }


    /**
     * Generates and returns an EC KeyPair.
     * <p> If on Android SDK >=34 the KeyPair is stored in the Keystore.</p>
     *
     * <p> If on Android SDK <34 the KeyPair is returned.</p>
     * @param keystoreAlias
     * @return
     * @throws GeneralSecurityException
     * @throws InterruptedException
     * @throws IOException
     */
    public static Pair<KeyPair, byte[]> generateKeyPair(String keystoreAlias) throws
            GeneralSecurityException, InterruptedException, IOException {
        /*
         * Generate a new EC key pair entry in the Android Keystore by
         * using the KeyPairGenerator API. The private key can only be
         * used for signing or verification and only with SHA-256 or
         * SHA-512 as the message digest.
         */
//        KeyPair keyPair;
        Pair<KeyPair, byte[]> keyPairPair;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // puts into the keystore
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(new KeyGenParameterSpec.Builder(
                    keystoreAlias,
                    KeyProperties.PURPOSE_AGREE_KEY)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .build());

            keyPairPair = new Pair<>(kpg.generateKeyPair(), null);
        } else {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(256);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            byte[] encryptedPrivateKey = keystoreDeriveEncryptedPrivateKey(keyPair, keystoreAlias);
            keyPairPair = new Pair<>(keyPair, encryptedPrivateKey);
        }
        return keyPairPair;
    }

//    /**
//     *
//     * Generates and returns an EC KeyPair from another PublicKey params.
//     * <p> If on Android SDK >=34 the KeyPair is stored in the Keystore.</p>
//     *
//     * <p> If on Android SDK <34 the KeyPair is returned.</p>
//     * @param keystoreAlias
//     * @param publicKey
//     * @return
//     * @throws GeneralSecurityException
//     */
//    public static Pair<KeyPair, byte[]> generateKeyPair(String keystoreAlias, PublicKey publicKey)
//            throws GeneralSecurityException {
//        Pair<KeyPair, byte[]> keyPairPair;
//
//        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ) {
//            Log.d(SecurityECDH.class.getName(), "Public key name: " + publicKey.getAlgorithm());
//            ECParameterSpec dhParameterSpec = ((ECPublicKey) publicKey).getParams();
//            ECGenParameterSpec paramSpec = new ECGenParameterSpec(publicKey.getAlgorithm());
//            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
//            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(keystoreAlias,
//                    KeyProperties.PURPOSE_AGREE_KEY)
//                    .setAlgorithmParameterSpec(paramSpec)
//                    .build());
//            keyPairPair = new Pair<>(keyPairGenerator.generateKeyPair(), null);
//        } else {
//            ECParameterSpec dhParameterSpec = ((BCECPublicKey) publicKey).getParams();
//            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM, PROVIDER);
//            keyPairGenerator.initialize(dhParameterSpec);
//            KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//            byte[] encryptedPrivateKey = keystoreDeriveEncryptedPrivateKey(keyPair, keystoreAlias);
//            keyPairPair = new Pair<>(keyPair, encryptedPrivateKey);
//        }
//        return keyPairPair;
//    }

    private static byte[] keystoreDeriveEncryptedPrivateKey(KeyPair keyPair, String keystoreAlias) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, NoSuchProviderException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance( KeyProperties.KEY_ALGORITHM_RSA,
                "AndroidKeyStore");
        kpg.initialize(new KeyGenParameterSpec.Builder(keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build());

        KeyPair keystoreKeyPair = kpg.generateKeyPair();
        return SecurityRSA.encrypt(keystoreKeyPair.getPublic(), keyPair.getPrivate().getEncoded());
    }


}
