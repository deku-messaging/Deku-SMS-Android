package com.example.swob_deku.Models.Security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
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

    public KeyPair keypair;
    public KeyAgreement keyAgree;

    public final static String DEFAULT_PROVIDER = "AndroidKeyStore";
    public final String DEFAULT_ALGORITHM = "DH";

    public final int DEFAULT_KEY_SIZE = 512;
    public SecurityDH() throws NoSuchAlgorithmException {
        this.keyAgree  = KeyAgreement.getInstance(DEFAULT_ALGORITHM);
    }

    public void keyAgreement(byte[] publicKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
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

        this.keyAgree.init(this.keypair.getPrivate());
        this.keyAgree.doPhase(publicKey, true);
    }

    public void generateKeyPair(String keyAlias) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        // TODO: check if keypair already exist

//        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);
//        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,
//                DEFAULT_PROVIDER);

        // TODO:
//        keyGenerator.initialize(DEFAULT_KEY_SIZE);

        // TODO: this works only for android 31 and above
//        keyGenerator.initialize(
//                new KeyGenParameterSpec.Builder(
//                        keyAlias, KeyProperties.PURPOSE_AGREE_KEY)
//                        .setKeySize(DEFAULT_KEY_SIZE)
//                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
//                        .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
//                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
//                        .build());
//        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(msisdnAsAlias,
//                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
//                .setDigests(KeyProperties.DIGEST_SHA256)
//                .setKeySize(2048);
//        keyGenerator.initialize(builder.build());
//
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(); // Replace with the curve of your choice
            keyPairGenerator.initialize(ecSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            // handle exception
        }

        keyGenerator.initialize(DEFAULT_KEY_SIZE);
        this.keypair = keyGenerator.generateKeyPair();
        this.keyAgree.init(this.keypair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
//        byte[] alicePubKeyEnc = this.keypair.getPublic().getEncoded();
//
//        return alicePubKeyEnc;
    }

    public void generateKeyPairFromPublicKey(byte[] publicKeyEnc, String msisdnAsAlias) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
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
//        keyPairGenerator.initialize(dhParameterSpec);

//        keyPairGenerator.initialize(
//                new KeyGenParameterSpec.Builder(
//                        msisdnAsAlias,
//                        KeyProperties.PURPOSE_DECRYPT
//                                | KeyProperties.PURPOSE_ENCRYPT
//                                | KeyProperties.PURPOSE_SIGN)
//                        .setAlgorithmParameterSpec(dhParameterSpec)
//                        .setKeySize(DEFAULT_KEY_SIZE)
//                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
//                        .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
//                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
//                        .build());

        this.keypair = keyPairGenerator.generateKeyPair();

        // Bob encodes his public key, and sends it over to Alice.
//        byte[] bobPubKeyEnc = this.keypair.getPublic().getEncoded();
//
//        return bobPubKeyEnc;
    }


    public byte[] generateSecretKey() {
        return this.keyAgree.generateSecret();
    }


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
