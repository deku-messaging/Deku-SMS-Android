package com.afkanerd.deku.E2EE.Security;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;


import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.provider.PEMUtil;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemObjectParser;

import java.io.IOException;
import java.io.StringReader;
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
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class SecurityECDH {
    public final static String DEFAULT_ALGORITHM = "ECDH";

    public final int DEFAULT_KEY_SIZE = 256;
    public static final String PROVIDER = "SC";


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public boolean isAvailableInKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.containsAlias(keystoreAlias);
    }

    public static int removeFromCustomKeystore(Context context, String keystoreAlias) throws InterruptedException {
        CustomKeyStoreDao customKeyStoreDao = CustomKeyStore.getDao(context);
        final int[] numberUpdated = {0};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                numberUpdated[0] = customKeyStoreDao.delete(keystoreAlias);
            }
        });

        thread.start();
        thread.join();

        return numberUpdated[0];
    }

    public int removeFromKeystore(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(keystoreAlias);

        return removeFromCustomKeystore(context, keystoreAlias);
    }

    public static PublicKey buildPublicKey(byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(publicKey);
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

    public static byte[] generateSecretKey(Context context, String keystoreAlias, PublicKey publicKey)
            throws GeneralSecurityException, IOException, InterruptedException {

        final PrivateKey[] privateKey = new PrivateKey[1];
        if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            CustomKeyStoreDao customKeyStoreDao = CustomKeyStore.getDao(context);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomKeyStore customKeyStore = customKeyStoreDao.find(keystoreAlias);
                    try {
                        privateKey[0] = customKeyStore.buildPrivateKey();
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException |
                             UnrecoverableKeyException | KeyStoreException | CertificateException |
                             IOException | NoSuchPaddingException | IllegalBlockSizeException |
                             BadPaddingException | InvalidKeyException |
                             InvalidAlgorithmParameterException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();
            thread.join();
        }
        else {
            privateKey[0] = getPrivateKeyFromKeystore(keystoreAlias);
        }

        KeyAgreement keyAgreement = KeyAgreement.getInstance(DEFAULT_ALGORITHM);
        keyAgreement.init(privateKey[0]);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    public static void storeInCustomKeyStore(Context context, String keystoreAlias, KeyPair keyPair)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            InterruptedException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
        kpg.initialize(new KeyGenParameterSpec.Builder(keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build());

        KeyPair keystoreKeyPair = kpg.generateKeyPair();
        byte[] encryptedPrivateKey = SecurityRSA.encrypt(keystoreKeyPair.getPublic(),
                keyPair.getPrivate().getEncoded());

        CustomKeyStore customKeyStore = new CustomKeyStore();
        customKeyStore.setPrivateKey(Base64.encodeToString(encryptedPrivateKey, Base64.DEFAULT));
        customKeyStore.setPublicKey(Base64.encodeToString(keyPair.getPublic().getEncoded(),
                Base64.DEFAULT));
        customKeyStore.setKeystoreAlias(keystoreAlias);
        CustomKeyStoreDao customKeyStoreDao = CustomKeyStore.getDao(context);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(getClass().getName(), "Number inserted: " + customKeyStoreDao.insert(customKeyStore));
            }
        });
        thread.start();
        thread.join();
    }

    public static PublicKey generateKeyPair(Context context, String keystoreAlias) throws
            GeneralSecurityException, InterruptedException {
        /*
         * Generate a new EC key pair entry in the Android Keystore by
         * using the KeyPairGenerator API. The private key can only be
         * used for signing or verification and only with SHA-256 or
         * SHA-512 as the message digest.
         */
        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // puts into the keystore
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(new KeyGenParameterSpec.Builder(
                    keystoreAlias,
                    KeyProperties.PURPOSE_AGREE_KEY)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .build());

            return kpg.generateKeyPair().getPublic();
        } else {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(256);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            storeInCustomKeyStore(context, keystoreAlias, keyPair);

            return keyPair.getPublic();
        }
    }


    public static KeyPair generateKeyPairFromPublicKey(
            Context context, String keystoreAlias, PublicKey publicKey) throws GeneralSecurityException, InterruptedException {
        ECParameterSpec dhParameterSpec = ((BCECPublicKey)publicKey).getParams();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        keyPairGenerator.initialize(dhParameterSpec);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        storeInCustomKeyStore(context, keystoreAlias, keyPair);

        return keyPair;
    }

}
