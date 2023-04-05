package com.example.swob_deku.Models.Security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.swob_deku.BuildConfig;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
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
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
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

    public byte[] getSecretKey(byte[] publicKeyEnc, String alias) throws GeneralSecurityException, IOException {
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

    private X509Certificate generateCertificate(KeyPair keyPair) throws NoSuchAlgorithmException, InvalidKeyException, IOException, CertificateException, OperatorCreationException, InvalidKeySpecException {
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

    public PublicKey generateKeyPair(Activity activity, Context context, String keystoreAlias) throws GeneralSecurityException, IOException, OperatorCreationException {
        // TODO: check if keypair already exist
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);

        // TODO: this works only for android 31 and above
        keyGenerator.initialize(DEFAULT_KEY_SIZE);
        KeyPair keypair = keyGenerator.generateKeyPair();

        securelyStoreKeyPair(context, keystoreAlias, keypair);
        return keypair.getPublic();
    }

    private PrivateKey securelyFetchPrivateKey(String keystoreAlias) throws GeneralSecurityException, IOException {
        keystoreAlias += "-private-key";
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                this.masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        String encryptedSharedKey = encryptedSharedPreferences.getString(
                keystoreAlias, "");

        byte[] privateKeyDecoded = Base64.decode(encryptedSharedKey, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM); // Replace "RSA" with your key algorithm
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyDecoded);

        return keyFactory.generatePrivate(keySpec);
    }

    private void securelyStoreKeyPair(Context context, String keystoreAlias, KeyPair keyPair) throws GeneralSecurityException, IOException, OperatorCreationException {

        // TODO: make alias know it's private key stored now
        keystoreAlias += "-private-key";

        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        SharedPreferences.Editor sharedPreferencesEditor = encryptedSharedPreferences.edit();

        sharedPreferencesEditor.putString(keystoreAlias,
                Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));

        if(!sharedPreferencesEditor.commit()) {
            throw new RuntimeException("Failed to store MSISDN");
        }
    }

    public PublicKey generateKeyPairFromPublicKey(byte[] publicKeyEnc, String msisdnAsAlias) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
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

        KeyPair keypair = keyPairGenerator.generateKeyPair();

        // Bob encodes his public key, and sends it over to Alice.
//        byte[] bobPubKeyEnc = this.keypair.getPublic().getEncoded();
//
//        return bobPubKeyEnc;
        return keypair.getPublic();
    }


//    public byte[] generateSecretKey() throws NoSuchAlgorithmException {
//        KeyAgreement keyAgree  = KeyAgreement.getInstance(DEFAULT_ALGORITHM);
//        return keyAgree.generateSecret();
//    }


    public static List<byte[]> encryptAES(byte[] plainText, byte[] secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        /*
         * Now let's create a SecretKey object using the shared secret
         * and use it for encryption. First, we generate SecretKeys for the
         * "AES" algorithm (based on the raw shared secret data) and
         * Then we use AES in CBC mode, which requires an initialization
         * vector (IV) parameter. Note that you have to use the same IV
         * for encryption and decryption: If you use a different IV for
         * decryption than you used for encryption, decryption will fail.
         *
         * If you do not specify an IV when you initialize the Cipher
         * object for encryption, the underlying implementation will generate
         * a random one, which you have to retrieve using the
         * javax.crypto.Cipher.getParameters() method, which returns an
         * instance of java.security.AlgorithmParameters. You need to transfer
         * the contents of that object (e.g., in encoded format, obtained via
         * the AlgorithmParameters.getEncoded() method) to the party who will
         * do the decryption. When initializing the Cipher for decryption,
         * the (reinstantiated) AlgorithmParameters object must be explicitly
         * passed to the Cipher.init() method.
         */
//        Log.i(this.getClass().getName(), "Use shared secret as SecretKey object ...");
        SecretKeySpec bobAesKey = new SecretKeySpec(secretKey, 0, 16, "AES");

        /*
         * Bob encrypts, using AES in CBC mode
         */
        Cipher bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        bobCipher.init(Cipher.ENCRYPT_MODE, bobAesKey);
        byte[] ciphertext = bobCipher.doFinal(plainText);

        // Retrieve the parameter that was used, and transfer it to Alice in
        // encoded format
        // byte[] encodedParams = bobCipher.getParameters().getEncoded();
        byte[] iv = bobCipher.getIV();

        List<byte[]> ivText = new ArrayList<>();
        ivText.add(ciphertext);
        ivText.add(iv);

        return ivText;
    }

    public boolean hasEncryption(String keystoreAlias) throws GeneralSecurityException, IOException {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences.create(
                context,
                keystoreAlias,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM );

        return encryptedSharedPreferences.contains(keystoreAlias);
    }


    public static byte[] decryptAES(byte[] secretKey, byte[] ciphertext, byte[] iv) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        /*
         * Alice decrypts, using AES in CBC mode
         */

        // Instantiate AlgorithmParameters object from parameter encoding
        // obtained from Bob
        SecretKeySpec sharedKey = new SecretKeySpec(secretKey, 0, 16, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher aliceCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aliceCipher.init(Cipher.DECRYPT_MODE, sharedKey, ivParameterSpec);
        byte[] recovered = aliceCipher.doFinal(ciphertext);

        return recovered;
    }

    public static void test() throws Exception {
//        SecurityDH alice = new SecurityDH();
//        SecurityDH bob = new SecurityDH();
//
//        byte[] alicePubKeyEnc = alice.generateKeyPair();
//        byte[] bobPubKeyEnc = bob.generateKeyPairFromPublicKey(alicePubKeyEnc);
//
//        byte[] aliceSharedSecret = alice.DHKeyAgreement(bobPubKeyEnc, alice.keypair)
//                .generateSecretKey();
//        byte[] bobSharedSecret = bob.DHKeyAgreement(alicePubKeyEnc, bob.keypair)
//                .generateSecretKey();
//
//        Log.i(SecurityDH.class.getName(), "Alice secret key:\n" + new String(aliceSharedSecret, "UTF-8"));
//        SecretKeySpec key = new SecretKeySpec(aliceSharedSecret,"AES");
//        Log.i(SecurityDH.class.getName(), "Alice secret key:\n" + Base64.encodeToString(key.getEncoded(), Base64.DEFAULT));
//
//        /*
//        Log.i(DHKeyAgreement2.class.getName(), "Alice secret: " +
//                Helpers.toHexString(aliceSharedSecret));
//        Log.i(DHKeyAgreement2.class.getName(), "Bob secret: " +
//                Helpers.toHexString(bobSharedSecret));
//
//         */
//        if (!java.util.Arrays.equals(aliceSharedSecret, bobSharedSecret))
//            throw new Exception("Shared secrets differ");
//        Log.i(SecurityDH.class.getName(), "Shared secrets are the same");
//
//        List<byte[]> ivText = SecurityDH.encryptAES("Hello world".getBytes(StandardCharsets.UTF_8), aliceSharedSecret);
//        Log.i(SecurityDH.class.getName(), "Encrypted Cipher Text: \n" + Base64.encodeToString(ivText.get(0), Base64.DEFAULT));
//        Log.i(SecurityDH.class.getName(), "Encrypted IV: \n" + Base64.encodeToString(ivText.get(1), Base64.DEFAULT));
//        Log.i(SecurityDH.class.getName(), "Encrypted String: \n" + new String(SecurityDH.decryptAES(aliceSharedSecret, ivText.get(0), ivText.get(1)), "UTF-8"));
    }

}
