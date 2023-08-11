package com.example.swob_deku.Models.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

//import org.bouncycastle.operator.OperatorCreationException;

import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityECDH {

    public final static String DEFAULT_PROVIDER = "AndroidKeyStore";
//    public final String DEFAULT_ALGORITHM = "DH";
    public final String DEFAULT_ALGORITHM = "ECDH";

//    public final int DEFAULT_KEY_SIZE = 512;
    public final int DEFAULT_KEY_SIZE = 256;

//    private static final String PROVIDER = "BC";
    private static final String PROVIDER = "SC";

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PATH = "keystore.p12";

    public static final String UNIVERSAL_KEYSTORE_ALIAS = "UNIVERSAL_KEYSTORE_ALIAS";
    private static final String UNIVERSAL_PRIVATE_KEY_ALIAS = "UNIVERSAL_PRIVATE_KEY_ALIAS";
    private static final String UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS = "UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS";

    MasterKey masterKeyAlias;

    Context context;
    public SecurityECDH(Context context) throws GeneralSecurityException, IOException {
        this.context = context;

        this.masterKeyAlias = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

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

    public KeyPair generateKeyPair() throws GeneralSecurityException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM, PROVIDER);
        keyPairGenerator.initialize(DEFAULT_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    public PrivateKey securelyFetchPrivateKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_PRIVATE_KEY_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        String encryptedSharedKey = encryptedSharedPreferences.getString(
                keystoreAlias, "");

        byte[] privateKeyDecoded = Base64.decode(encryptedSharedKey, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM); // Replace "RSA" with your key algorithm
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyDecoded);

        return keyFactory.generatePrivate(keySpec);
    }

    public String securelyFetchSecretKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.getString( keystoreAlias, "");
    }

    public Map<String, ?> securelyFetchAllSecretKey() throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.getAll();
    }

    public boolean hasPrivateKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_PRIVATE_KEY_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
    }

    public boolean hasSecretKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
    }

    public void securelyStorePrivateKeyKeyPair(Context context, String keystoreAlias, KeyPair keyPair) throws GeneralSecurityException, IOException {
        // TODO: make alias know it's private key stored now
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_PRIVATE_KEY_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();
        String privateKey = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);
        sharedPreferencesEditor.putString(keystoreAlias, privateKey).commit();

        Log.d(getClass().getName(), "Securely stored private key!");
    }

    public String securelyStorePeerAgreementKey(Context context, String keystoreAlias, byte[] keyValue) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();

        byte[] merged = SecurityHelpers.rxAgreementFormatter(keyValue);
        String returnString = Base64.encodeToString(merged, Base64.DEFAULT);

        removeSecretKey(keystoreAlias);
        sharedPreferencesEditor.putString(keystoreAlias, returnString)
                .commit();
        Log.d(getClass().getName(), "Securely stored peer agreement key!");

        return returnString;
    }

    public void removeSecretKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferencesAgreementKeys = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditorAgreementKey =
                encryptedSharedPreferencesAgreementKeys.edit();

        sharedPreferencesEditorAgreementKey
                .remove(keystoreAlias)
                .commit();

        SharedPreferences sharedPreferencesKeystore = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferencesKeystore.edit();
        sharedPreferencesEditor
                .remove(keystoreAlias)
                .commit();


    }

    public void removeAllKeys(String keystoreAlias) throws GeneralSecurityException, IOException {
        // remove secret keys
        SharedPreferences sharedPreferencesKeystore = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferencesKeystore.edit();
        sharedPreferencesEditor
                .remove(keystoreAlias)
                .commit();

        // remove private keys
        SharedPreferences sharedPreferencesPrivateKeystore = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_PRIVATE_KEY_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditorPrivateKey = sharedPreferencesPrivateKeystore.edit();
        sharedPreferencesEditorPrivateKey
                .remove(keystoreAlias)
                .commit();

        // remove agreement keys
        SharedPreferences encryptedSharedPreferencesAgreementKeys = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditorAgreementKey =
                encryptedSharedPreferencesAgreementKeys.edit();

        sharedPreferencesEditorAgreementKey
                .remove(keystoreAlias)
                .commit();
    }

    public boolean peerAgreementPublicKeysAvailable(Context context, String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
    }

    public String getPeerAgreementPublicKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_AGREEMENT_KEY_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.getString(keystoreAlias, "");
    }

    public void securelyStoreSecretKey(String keystoreAlias, byte[] secret) throws GeneralSecurityException, IOException {
        SharedPreferences universalSharedPreferences = EncryptedSharedPreferences.create(
                context,
                UNIVERSAL_KEYSTORE_ALIAS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = universalSharedPreferences.edit();
        sharedPreferencesEditor.
                putString(keystoreAlias, Base64.encodeToString(secret, Base64.DEFAULT))
                .apply();
//        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
//                context,
//                keystoreAlias,
//                masterKeyAlias,
//                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );
//
//        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();
//
//        sharedPreferencesEditor.clear()
//                .putString(keystoreAlias, Base64.encodeToString(secret, Base64.DEFAULT))
//                .apply();

    }

    public KeyPair generateKeyPairFromPublicKey(byte[] publicKeyEnc) throws GeneralSecurityException, IOException {
        KeyFactory bobKeyFac = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyEnc);

        PublicKey publicKey = bobKeyFac.generatePublic(x509KeySpec);

        ECParameterSpec dhParameterSpec = ((BCECPublicKey)publicKey).getParams();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        keyPairGenerator.initialize(dhParameterSpec);

        //        securelyStorePrivateKeyKeyPair(context, alias, keyPair);
        return keyPairGenerator.generateKeyPair();
    }

//    public static byte[] encryptAES(byte[] input, byte[] secretKey) throws Throwable {
//        try {
//            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, 16, "AES");
//
//            Cipher cipher = Cipher.getInstance(SecurityAES.DEFAULT_AES_ALGORITHM);
//            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
//            byte[] ciphertext = cipher.doFinal(input);
//
//            byte[] cipherTextIv = new byte[16 + ciphertext.length];
//            System.arraycopy(cipher.getIV(), 0,  cipherTextIv, 0, 16);
//            System.arraycopy(ciphertext, 0,  cipherTextIv, 16, ciphertext.length);
//
//            return cipherTextIv;
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            throw new Throwable(e);
//        }
//    }
//
//    public static byte[] decryptAES(byte[] input, byte[] secretKey) throws Throwable {
//        try {
//            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, 16, "AES");
//
//            byte[] iv = new byte[16];
//            System.arraycopy(input, 0, iv, 0, 16);
//
//            byte[] content = new byte[input.length - 16];
//            System.arraycopy(input, 16, content, 0, content.length);
//
//            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
//
//            Cipher cipher = Cipher.getInstance(SecurityAES.DEFAULT_AES_ALGORITHM);
//            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
//            return cipher.doFinal(content);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            throw new Throwable(e);
//        }
//    }
}
