package com.afkanerd.deku.E2EE;


import static com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers.storeInCustomKeyStore;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.E2EE.Security.CustomKeyStore;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;
import com.afkanerd.deku.E2EE.Security.EncryptionHandlers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityAES;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityECDH;
import com.google.common.primitives.Bytes;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
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
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
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
                Log.d(getClass().getName(), "Number inserted: " + customKeyStoreDao.insert(customKeyStore));
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
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        return conversationsThreadsEncryptionDao.delete(keystoreAlias);
    }

    public static boolean isValidDekuPublicKey(byte[] dekuPublicKey) {
        byte[] start = EncryptionHandlers.dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] end = EncryptionHandlers.dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8);

        int indexStart = Bytes.indexOf(dekuPublicKey, start);
        int indexEnd = Bytes.indexOf(dekuPublicKey, end);

        if(indexStart == 0 && indexEnd == ((dekuPublicKey.length - end.length))) {
            byte[] keyValue = new byte[dekuPublicKey.length - (start.length + end.length)];
            System.arraycopy(dekuPublicKey, start.length, keyValue, 0, keyValue.length);

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

    public static boolean isValidDekuText(String text) {
        try {
            String encodedText = text.substring(EncryptionHandlers.dekuTextStartPrefix.length(),
                    (text.length() - EncryptionHandlers.dekuTextEndPrefix.length()));
            return text.startsWith(EncryptionHandlers.dekuTextStartPrefix) &&
                    text.endsWith(EncryptionHandlers.dekuTextEndPrefix) &&
                    Helpers.isBase64Encoded(encodedText);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] buildDekuPublicKey(byte[] data) {
//        return EncryptionHandlers.convertPublicKeyToPEMFormat(data);
        return EncryptionHandlers.convertPublicKeyToDekuFormat(data);
    }

    public static byte[] extractTransmissionKey(byte[] data) {
        byte[] start = EncryptionHandlers.dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] end = EncryptionHandlers.dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8);

        byte[] transmissionKey = new byte[data.length - (start.length + end.length)];
        System.arraycopy(data, start.length, transmissionKey, 0, transmissionKey.length);
        return transmissionKey;
    }

    public static byte[] buildForEncryptionRequest(Context context, String address) throws NumberParseException, GeneralSecurityException, IOException, InterruptedException {
        int session = 0;
        String keystoreAlias = deriveKeystoreAlias(address, session);
        PublicKey publicKey = createNewKeyPair(context, keystoreAlias);
        return buildDekuPublicKey(publicKey.getEncoded());
    }

    public static long insertNewPeerPublicKey(Context context, byte[] publicKey, String keystoreAlias) {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption.setPublicKey(Base64.encodeToString(publicKey, Base64.DEFAULT));
        conversationsThreadsEncryption.setExchangeDate(System.currentTimeMillis());
        conversationsThreadsEncryption.setKeystoreAlias(keystoreAlias);
        return conversationsThreadsEncryptionDao.insert(conversationsThreadsEncryption);
    }

    public static ConversationsThreadsEncryption fetchPeerPublicKey(Context context, String keystoreAlias) {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        return conversationsThreadsEncryptionDao.fetch(keystoreAlias);
    }

    public static byte[] encryptText(Context context, String keystoreAlias, String text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));

        final KeyPair[] keyPair = new KeyPair[1];
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomKeyStore customKeyStore = new CustomKeyStore();
                    CustomKeyStoreDao customKeyStoreDao = customKeyStore.getDaoInstance(context);
                    CustomKeyStore customKeyStore1 = customKeyStoreDao.find(keystoreAlias);
                    try {
                        keyPair[0] = customKeyStore1.getKeyPair();
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
        if(keyPair[0] != null) {
            byte[] _secretKey = SecurityECDH.generateSecretKey(keyPair[0], publicKey);
            SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
            return SecurityAES.encryptAESGCM(text.getBytes(StandardCharsets.UTF_8), secretKey);
        }
        return null;
    }

    public static byte[] decryptText(Context context, String keystoreAlias, byte[] text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));

        final KeyPair[] keyPair = new KeyPair[1];
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomKeyStore customKeyStore = new CustomKeyStore();
                    CustomKeyStoreDao customKeyStoreDao = customKeyStore.getDaoInstance(context);
                    CustomKeyStore customKeyStore1 = customKeyStoreDao.find(keystoreAlias);
                    try {
                        keyPair[0] = customKeyStore1.getKeyPair();
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
        if(keyPair[0] != null) {
            byte[] _secretKey = SecurityECDH.generateSecretKey(keyPair[0], publicKey);
            SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
            return SecurityAES.decryptAESGCM(text, secretKey);
        }
        return null;
    }

    public static String buildTransmissionText(byte[] data) {
        return EncryptionHandlers.convertTextToDekuFormat(data);
    }

    public static byte[] extractTransmissionText(String text) {
        String encodedText = text.substring(EncryptionHandlers.dekuTextStartPrefix.length(),
                (text.length() - EncryptionHandlers.dekuTextEndPrefix.length()));

        return Base64.decode(encodedText, Base64.DEFAULT);
    }


    public final static int REQUEST_KEY = 0;
    public final static int AGREEMENT_KEY = 1;
    public final static int IGNORE_KEY = 2;
    public static int getKeyType(Context context, String keystoreAlias, byte[] publicKey) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if(isAvailableInKeystore(keystoreAlias)) {
            ConversationsThreadsEncryption conversationsThreadsEncryption =
                    fetchPeerPublicKey(context, keystoreAlias);
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
