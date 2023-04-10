package com.example.swob_deku.Models.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

//import org.bouncycastle.operator.OperatorCreationException;

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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityDH {

    public final static String DEFAULT_PROVIDER = "AndroidKeyStore";
    public final String DEFAULT_ALGORITHM = "DH";

    public final int DEFAULT_KEY_SIZE = 512;

    private static final String PROVIDER = "BC";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PATH = "keystore.p12";

    MasterKey masterKeyAlias;

    Context context;
    public SecurityDH(Context context) throws GeneralSecurityException, IOException {
        this.context = context;

        this.masterKeyAlias = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
    }

    public byte[] generateSecretKey(byte[] publicKeyEnc, String alias) throws GeneralSecurityException, IOException {
        /*
         * Alice uses Bob's public key for the first (and only) phase
         * of her version of the DH
         * protocol.
         * Before she can do so, she has to instantiate a DH public key
         * from Bob's encoded key material.
         */
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyEnc);
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);

        PrivateKey privateKey = securelyFetchPrivateKey(alias);

        KeyAgreement keyAgree  = KeyAgreement.getInstance(DEFAULT_ALGORITHM);

        keyAgree.init(privateKey);
        keyAgree.doPhase(publicKey, true);

        return keyAgree.generateSecret();
    }

    public PublicKey generateKeyPair(Context context, String keystoreAlias) throws GeneralSecurityException, IOException {
        // TODO: check if keypair already exist
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        // TODO: this works only for android 31 and above
        keyGenerator.initialize(DEFAULT_KEY_SIZE);
        KeyPair keypair = keyGenerator.generateKeyPair();

        securelyStorePrivateKeyKeyPair(context, keystoreAlias, keypair);
        return keypair.getPublic();
    }

    public PrivateKey securelyFetchPrivateKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        String encryptedSharedKey = encryptedSharedPreferences.getString(
                keystoreAlias + "-private-key", "");

        byte[] privateKeyDecoded = Base64.decode(encryptedSharedKey, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM); // Replace "RSA" with your key algorithm
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyDecoded);

        return keyFactory.generatePrivate(keySpec);
    }

    public String securelyFetchSecretKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.getString( keystoreAlias, "");
    }

    public boolean hasPrivateKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias + "-private-key");
    }

    public boolean hasSecretKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
    }

    public void securelyStorePrivateKeyKeyPair(Context context, String keystoreAlias, KeyPair keyPair) throws GeneralSecurityException, IOException {
        // TODO: make alias know it's private key stored now
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();

        sharedPreferencesEditor.putString(keystoreAlias + "-private-key",
                Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));

        if(!sharedPreferencesEditor.commit()) {
            throw new RuntimeException("Failed to store MSISDN");
        } else {
            Log.d(SecurityDH.class.getName(), "Securely stored private key");
        }
    }

    public String securelyStorePublicKeyKeyPair(Context context, String keystoreAlias, byte[] keyValue, int part) throws GeneralSecurityException, IOException {
        // TODO: make alias know it's private key stored now
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();

        String formattedKeystoreAlias = keystoreAlias + "-public-key-" + part;
        String otherFormattedKeystoreAlias = keystoreAlias + "-public-key-" + (part == 1 ? 0 : 1);

        String returnString = "";
        if(encryptedSharedPreferences.contains(otherFormattedKeystoreAlias)) {
            // TODO: build the key now
            String otherPart = encryptedSharedPreferences.getString(otherFormattedKeystoreAlias, "");

            byte[] otherPartByte = Base64.decode(otherPart, Base64.DEFAULT);

            byte[][] rxMergeNeeded =  part == 0?
                    new byte[][]{keyValue, otherPartByte} : new byte[][]{otherPartByte, keyValue};

            byte[] merged = SecurityHelpers.rxAgreementFormatter(rxMergeNeeded);

            returnString = Base64.encodeToString(merged, Base64.DEFAULT);
            sharedPreferencesEditor
                    .remove(otherFormattedKeystoreAlias)
                    .remove(formattedKeystoreAlias)
                    .putString(keystoreAlias + "-agreement-key", returnString);

            if(!sharedPreferencesEditor.commit()) {
                throw new RuntimeException("Failed to store merged agreement");
            }
        } else {
            returnString = Base64.encodeToString(keyValue, Base64.DEFAULT);
            sharedPreferencesEditor.putString(formattedKeystoreAlias, returnString);

            if (!sharedPreferencesEditor.commit()) {
                throw new RuntimeException("Failed to store public key part");
            }
        }

        return returnString;
    }

    public void removeAllKeys(String keystoreAlias) throws GeneralSecurityException, IOException {
        Log.d(SecurityDH.class.getName(), "Removing preferences for: " + keystoreAlias);
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();
        sharedPreferencesEditor.clear().commit();

    }

    public boolean peerAgreementPublicKeysAvailable(Context context, String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias + "-agreement-key");
    }

    public String getPeerAgreementPublicKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.getString(keystoreAlias + "-agreement-key", "");
    }

    public void securelyStoreSecretKey(String keystoreAlias, byte[] secret) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();

        sharedPreferencesEditor.clear()
                .putString(keystoreAlias, Base64.encodeToString(secret, Base64.DEFAULT))
                .commit();
    }

    public KeyPair generateKeyPairFromPublicKey(byte[] publicKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        KeyFactory bobKeyFac = KeyFactory.getInstance(DEFAULT_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyEnc);

        PublicKey publicKey = bobKeyFac.generatePublic(x509KeySpec);

        /*
         * Bob gets the DH parameters associated with Alice's public key.
         * He must use the same parameters when he generates his own key
         * pair.
         */
        DHParameterSpec dhParameterSpec = ((DHPublicKey)publicKey).getParams();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        keyPairGenerator.initialize(dhParameterSpec);

        return keyPairGenerator.generateKeyPair();

        // Bob encodes his public key, and sends it over to Alice.
//        byte[] bobPubKeyEnc = this.keypair.getPublic().getEncoded();
//
//        return bobPubKeyEnc;
    }

//    public byte[] generateSecretKey() throws NoSuchAlgorithmException {
//        KeyAgreement keyAgree  = KeyAgreement.getInstance(DEFAULT_ALGORITHM);
//        return keyAgree.generateSecret();
//    }
    public static byte[] encryptAES(byte[] input, byte[] secretKey) throws Throwable {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, 16, "AES");

            Cipher cipher = Cipher.getInstance(SecurityAES.DEFAULT_AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] ciphertext = cipher.doFinal(input);

            byte[] cipherTextIv = new byte[16 + ciphertext.length];
            System.arraycopy(cipher.getIV(), 0,  cipherTextIv, 0, 16);
            System.arraycopy(ciphertext, 0,  cipherTextIv, 16, ciphertext.length);

            return cipherTextIv;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
    }

    public boolean hasEncryption(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
//        String keystorevalue = encryptedSharedPreferences.getString(keystoreAlias, "");
//        Log.d(getClass().getName(), "Got keystore value: " + keystorevalue);
//
//        return !keystorevalue.isEmpty();
    }

    public static byte[] decryptAES(byte[] input, byte[] secretKey) throws Throwable {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, 0, 16, "AES");

            byte[] iv = new byte[16];
            System.arraycopy(input, 0, iv, 0, 16);

            byte[] content = new byte[input.length - 16];
            System.arraycopy(input, 16, content, 0, content.length);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(SecurityAES.DEFAULT_AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(content);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new Throwable(e);
        }
    }

}
