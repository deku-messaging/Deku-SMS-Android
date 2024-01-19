package com.afkanerd.deku.E2EE;


import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.E2EE.Security.CustomKeyStore;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityAES;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityECDH;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Headers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Ratchets;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.States;
import com.google.common.primitives.Bytes;
import com.google.i18n.phonenumbers.NumberParseException;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class E2EEHandler {

    // DONT_CARE_ENOUGH_MESSAGED_FIRST => Headers information
    public final static String DEFAULT_HEADER_START_PREFIX = "HDEKU{";
    public final static String DEFAULT_HEADER_END_PREFIX = "}UKEDH";
    public final static String DEFAULT_TEXT_START_PREFIX = "TDEKU{";
    public final static String DEFAULT_TEXT_END_PREFIX = "}UKEDT";

    public final static String DEFAULT_HEADER_START_PREFIX_SHORTER = "H{";
    public final static String DEFAULT_HEADER_END_PREFIX_SHORTER = "}H";
    public final static String DEFAULT_TEXT_START_PREFIX_SHORTER = "T{";
    public final static String DEFAULT_TEXT_END_PREFIX_SHORTER = "}T";


    public static String convertToDefaultTextFormat(byte[] data) {
        return DEFAULT_TEXT_START_PREFIX_SHORTER
                + Base64.encodeToString(data, Base64.DEFAULT) +
                DEFAULT_TEXT_END_PREFIX_SHORTER;
    }

    public static byte[] convertPublicKeyToDekuFormat(byte[] data) {
        return Bytes.concat(DEFAULT_HEADER_START_PREFIX.getBytes(StandardCharsets.UTF_8),
                data, DEFAULT_HEADER_END_PREFIX.getBytes(StandardCharsets.UTF_8));
    }

    public static String deriveKeystoreAlias(String address, int sessionNumber) throws NumberParseException {
        String[] addressDetails = Helpers.getCountryNationalAndCountryCode(address);
        String keystoreAliasRequirements = addressDetails[0] + addressDetails[1] + "_" + sessionNumber;
        return Base64.encodeToString(keystoreAliasRequirements.getBytes(), Base64.NO_WRAP);
    }

    public static String getAddressFromKeystore(String keystoreAlias) {
        String decodedAlias = new String(Base64.decode(keystoreAlias, Base64.DEFAULT),
                StandardCharsets.UTF_8);
        return "+" + decodedAlias.split("_")[0];
    }

    public static boolean isAvailableInKeystore(String keystoreAlias) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */
        return KeystoreHelpers.isAvailableInKeystore(keystoreAlias);
    }

    public static boolean canCommunicateSecurely(Context context, String keystoreAlias) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();

        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        return isAvailableInKeystore(keystoreAlias) &&
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias) != null;
    }

    public static PublicKey createNewKeyPair(Context context, String keystoreAlias)
            throws GeneralSecurityException, InterruptedException, IOException {
        Pair<KeyPair, byte[]> publicKeyPair = SecurityECDH.generateKeyPair(keystoreAlias);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            storeInCustomKeystore(context, keystoreAlias, publicKeyPair.first, publicKeyPair.second);
        }
        return publicKeyPair.first.getPublic();
    }

    private static void storeInCustomKeystore(Context context, String keystoreAlias, KeyPair keyPair,
                                      byte[] encryptedPrivateKey) throws InterruptedException {
        CustomKeyStore customKeyStore = new CustomKeyStore();
        customKeyStore.setPrivateKey(Base64.encodeToString(encryptedPrivateKey, Base64.DEFAULT));
        customKeyStore.setPublicKey(Base64.encodeToString(keyPair.getPublic().getEncoded(),
                Base64.DEFAULT));
        customKeyStore.setKeystoreAlias(keystoreAlias);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                CustomKeyStoreDao customKeyStoreDao = customKeyStore.getDaoInstance(context);
                customKeyStoreDao.insert(customKeyStore);
                customKeyStore.close();
            }
        });
        thread.start();
        thread.join();
    }

    public static void removeFromKeystore(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        KeystoreHelpers.removeFromKeystore(context, keystoreAlias);
        CustomKeyStore customKeyStore = new CustomKeyStore();
        new Thread(new Runnable() {
            @Override
            public void run() {
                CustomKeyStoreDao customKeyStoreDao = customKeyStore.getDaoInstance(context);
                customKeyStoreDao.delete(keystoreAlias);
                customKeyStore.close();
            }
        }).start();
    }

    public static int removeFromEncryptionDatabase(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        return conversationsThreadsEncryptionDao.delete(keystoreAlias);
    }

    public static boolean isValidDefaultPublicKey(byte[] publicKey) {
        // Backward compatibility - should be removed in later versions if can guarantee all users
        // migrated.
        byte[] start = DEFAULT_HEADER_START_PREFIX.getBytes(StandardCharsets.UTF_8);
        byte[] end = DEFAULT_HEADER_END_PREFIX.getBytes(StandardCharsets.UTF_8);

        byte[] startShorter = DEFAULT_HEADER_START_PREFIX_SHORTER.getBytes(StandardCharsets.UTF_8);
        byte[] endShorter = DEFAULT_HEADER_END_PREFIX_SHORTER.getBytes(StandardCharsets.UTF_8);

        int indexStart = Bytes.indexOf(publicKey, start);
        int indexEnd = Bytes.indexOf(publicKey, end);

        int indexStartShorter = Bytes.indexOf(publicKey, startShorter);
        int indexEndShorter = Bytes.indexOf(publicKey, endShorter);

        if(indexStart == 0 && indexEnd == ((publicKey.length - end.length))) {
            byte[] keyValue = new byte[publicKey.length - (start.length + end.length)];
            System.arraycopy(publicKey, start.length, keyValue, 0, keyValue.length);

            try {
                SecurityECDH.buildPublicKey(keyValue);
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else if(indexStartShorter == 0 &&
                indexEndShorter == ((publicKey.length - endShorter.length))) {
            byte[] keyValue = new byte[publicKey.length - (startShorter.length + endShorter.length)];
            System.arraycopy(publicKey, startShorter.length, keyValue, 0, keyValue.length);

            try {
                SecurityECDH.buildPublicKey(keyValue);
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isValidDefaultText(String text) {
        String encodedText;
        if(text.startsWith(DEFAULT_TEXT_START_PREFIX_SHORTER) &&
                text.endsWith(DEFAULT_TEXT_END_PREFIX_SHORTER))
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX_SHORTER.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX_SHORTER.length()));
        else
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX.length()));

        if(encodedText == null)
            return false;

        try {
            return Helpers.isBase64Encoded(encodedText);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isValidAliceText(String text) {
        return false;
    }

    public static boolean isValidBobText(String text) {
        return false;
    }

    public static byte[] buildDefaultPublicKey(byte[] data) {
//        return EncryptionHandlers.convertPublicKeyToPEMFormat(data);
        return convertPublicKeyToDekuFormat(data);
    }

    public static byte[] extractTransmissionKey(byte[] data) {
        byte[] start = DEFAULT_HEADER_START_PREFIX.getBytes(StandardCharsets.UTF_8);
        byte[] end = DEFAULT_HEADER_END_PREFIX.getBytes(StandardCharsets.UTF_8);

        byte[] transmissionKey = new byte[data.length - (start.length + end.length)];
        System.arraycopy(data, start.length, transmissionKey, 0, transmissionKey.length);
        return transmissionKey;
    }

    public static byte[] buildForEncryptionRequest(Context context, String address) throws NumberParseException, GeneralSecurityException, IOException, InterruptedException {
        int session = 0;
        String keystoreAlias = deriveKeystoreAlias(address, session);
        PublicKey publicKey = createNewKeyPair(context, keystoreAlias);
        return buildDefaultPublicKey(publicKey.getEncoded());
    }

    public static byte[] buildForEncryptionAgreement(Context context, String keystoreAlias) throws NumberParseException, GeneralSecurityException, IOException, InterruptedException {
        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        return buildDefaultPublicKey(keyPair.getPublic().getEncoded());
    }

    public static long insertNewAgreementKey(Context context, byte[] publicKey, String keystoreAlias) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption.setPublicKey(Base64.encodeToString(publicKey, Base64.DEFAULT));
        conversationsThreadsEncryption.setExchangeDate(System.currentTimeMillis());
        conversationsThreadsEncryption.setKeystoreAlias(keystoreAlias);

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        PublicKey publicKey1 = SecurityECDH.buildPublicKey(publicKey);
        final byte[] SK = SecurityECDH.generateSecretKey(keyPair, publicKey1);

        removeFromKeystore(context, keystoreAlias);

        States states = new States();
        byte[] output = Ratchets.ratchetInitAlice(keystoreAlias, states, SK,
                publicKey1);
        conversationsThreadsEncryption.setStates(states.getSerializedStates());

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            storeInCustomKeystore(context, keystoreAlias, states.DHs, output);

        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        return conversationsThreadsEncryptionDao.insert(conversationsThreadsEncryption);
    }

    public static long insertNewPeerPublicKey(Context context, byte[] publicKey, String keystoreAlias) throws GeneralSecurityException, IOException, InterruptedException, JSONException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption.setPublicKey(Base64.encodeToString(publicKey, Base64.DEFAULT));
        conversationsThreadsEncryption.setExchangeDate(System.currentTimeMillis());
        conversationsThreadsEncryption.setKeystoreAlias(keystoreAlias);

        Pair<KeyPair, byte[]> keyPairSecretOutput = SecurityECDH.generateKeyPair(keystoreAlias);
        final byte[] SK = SecurityECDH.generateSecretKey(keyPairSecretOutput.first,
                SecurityECDH.buildPublicKey(publicKey));
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            storeInCustomKeystore(context, keystoreAlias, keyPairSecretOutput.first,
                    keyPairSecretOutput.second);
        States states = Ratchets.ratchetInitBob(new States(), SK, keyPairSecretOutput.first);

        conversationsThreadsEncryption.setStates(states.getSerializedStates());
        return conversationsThreadsEncryption.getDaoInstance(context)
                .insert(conversationsThreadsEncryption);
    }

    public static ConversationsThreadsEncryption fetchStoredPeerData(Context context,
                                                                     String keystoreAlias) {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        return conversationsThreadsEncryptionDao.fetch(keystoreAlias);
    }

    public final static int ALICE_MESSAGE_FIRST_TYPE = 0;
    public final static int BOB_MESSAGE_FIRST_TYPE = 1;
    public final static int ALICE_AND_BOB_TALKS = 2;
    public final static int DONT_CARE_ENOUGH_MESSAGED_FIRST_TYPE = 3;

    public static byte[] encryptText(Context context, String keystoreAlias, String text) throws Exception {
        return defaultEncryptText(context, keystoreAlias, text);
    }

    public static KeyPair getKeyPairBasedVersioning(Context context, String keystoreAlias) throws UnrecoverableEntryException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        final KeyPair[] keyPair = new KeyPair[1];
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomKeyStore customKeyStore = new CustomKeyStore();
                    CustomKeyStoreDao customKeyStoreDao = customKeyStore.getDaoInstance(context);
                    customKeyStore = customKeyStoreDao.find(keystoreAlias);
                    try {
                        keyPair[0] = customKeyStore.getKeyPair();
                    } catch (UnrecoverableKeyException | InvalidKeySpecException |
                             InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                             NoSuchPaddingException | InvalidAlgorithmParameterException |
                             NoSuchAlgorithmException | IOException | KeyStoreException |
                             CertificateException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            thread.join();
        } else {
            keyPair[0] = KeystoreHelpers.getKeyPairFromKeystore(keystoreAlias);
        }
        return keyPair[0];
    }

    public static byte[] encrypt(Context context, String keystoreAlias, byte[] data) throws Throwable {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption = conversationsThreadsEncryption.getDaoInstance(context)
                .findByKeystoreAlias(keystoreAlias);
        String strStates = conversationsThreadsEncryption.getStates();

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        byte[] AD = Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT);
        Pair<Headers, byte[]> cipherPair = Ratchets.ratchetEncrypt(new States(keyPair, strStates),
                data, AD);
        return Bytes.concat(cipherPair.first.getSerialized(), cipherPair.second);
    }

    protected static byte[] defaultEncryptText(Context context, String keystoreAlias, String text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption = conversationsThreadsEncryption.getDaoInstance(context)
                .findByKeystoreAlias(keystoreAlias);

        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        if(keyPair != null) {
            byte[] _secretKey = SecurityECDH.generateSecretKey(keyPair, publicKey);
            SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
            return SecurityAES.encryptAESGCM(text.getBytes(StandardCharsets.UTF_8), secretKey);
        }
        return null;
    }

    public static byte[] decrypt(Context context, String keystoreAlias, byte[] cipherText) throws Throwable {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        Headers header = new Headers();
        byte[] outputCipherText = header.deSerializeHeader(cipherText);
        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        byte[] AD = keyPair.getPublic().getEncoded();
        States states = new States(keyPair, conversationsThreadsEncryption.getStates());
        byte[] decryptedText = Ratchets.ratchetDecrypt(keystoreAlias, states, header, outputCipherText, AD);

        // TODO: save states information
//        conversationsThreadsEncryption.setStates(states.getSerializedStates());
//        conversationsThreadsEncryptionDao.insert(conversationsThreadsEncryption);
        Log.d(E2EEHandler.class.getName(), states.getSerializedStates());
        return decryptedText;
    }

    public static byte[] decryptText(Context context, String keystoreAlias, byte[] text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption.getDaoInstance(context);
        conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
        if(keyPair != null) {
            byte[] _secretKey = SecurityECDH.generateSecretKey(keyPair, publicKey);
            SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
            return SecurityAES.decryptAESGCM(text, secretKey);
        }
        return null;
    }

    public static String buildTransmissionText(byte[] data) throws Exception {
        return convertToDefaultTextFormat(data);
    }

    public static byte[] extractTransmissionText(String text) throws Exception {
        String encodedText;
        if(text.startsWith(DEFAULT_TEXT_START_PREFIX_SHORTER) &&
                text.endsWith(DEFAULT_TEXT_END_PREFIX_SHORTER))
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX_SHORTER.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX_SHORTER.length()));
        else if(text.startsWith(DEFAULT_TEXT_START_PREFIX) && text.endsWith(DEFAULT_TEXT_END_PREFIX))
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX.length()));
        else
            throw new Exception("Invalid Transmission Text");

        return Base64.decode(encodedText, Base64.DEFAULT);
    }


    /**
     * ALICE_MESSAGE_FIRST messages contain Header, Ratchet Pubkey and Content
     *
     * BOB_MESSAGE_FIRST messages contain Ratchet PubKey and Content
     *
     * DONT_CARE_ENOUGH_MESSAGED_FIRST messages contain Content - not recommended, backward compatible
     *
     * @param message
     * @return
     */
    public static int getMessageType(String message){
        if(isValidAliceText(message)) {
            return ALICE_MESSAGE_FIRST_TYPE;
        } else if(isValidBobText(message)) {
            return BOB_MESSAGE_FIRST_TYPE;
        } else if(isValidDefaultText(message)) {
            return DONT_CARE_ENOUGH_MESSAGED_FIRST_TYPE;
        }
        return -1;
    }


    public final static int REQUEST_KEY = 0;
    public final static int AGREEMENT_KEY = 1;
    public final static int IGNORE_KEY = 2;
    public static int getKeyType(Context context, String keystoreAlias, byte[] publicKey) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if(isAvailableInKeystore(keystoreAlias)) {
            ConversationsThreadsEncryption conversationsThreadsEncryption =
                    fetchStoredPeerData(context, keystoreAlias);
            if(conversationsThreadsEncryption == null) {
                return AGREEMENT_KEY;
            }
            if(conversationsThreadsEncryption.getPublicKey().equals(
                    Base64.encodeToString(publicKey, Base64.DEFAULT))) {
                return IGNORE_KEY;
            }
        }
        return REQUEST_KEY;
    }
}
