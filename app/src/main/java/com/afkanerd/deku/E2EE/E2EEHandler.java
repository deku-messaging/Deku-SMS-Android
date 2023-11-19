package com.afkanerd.deku.E2EE;


import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.E2EE.Security.CustomKeyStore;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHandler;
import com.google.common.primitives.Bytes;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class E2EEHandler {

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }
    public static String getKeyStoreAlias(String address, int sessionNumber) throws NumberParseException {
        String[] addressDetails = Helpers.getCountryNationalAndCountryCode(address);
        String keystoreAliasRequirements = addressDetails[0] + addressDetails[1] + "_" + sessionNumber;
        return Base64.encodeToString(keystoreAliasRequirements.getBytes(), Base64.NO_WRAP);
    }

    public static boolean isAvailableInKeystore(String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */
        SecurityECDH securityECDH = new SecurityECDH();
        return securityECDH.isAvailableInKeystore(keystoreAlias);
    }

    public static boolean canCommunicateSecurely(Context context, String keystoreAlias) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        return isAvailableInKeystore(keystoreAlias) &&
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias) != null;
    }

    public static PublicKey createNewKeyPair(Context context, String keystoreAlias)
            throws GeneralSecurityException, InterruptedException, IOException {
        return SecurityECDH.generateKeyPair(context, keystoreAlias);
    }

    public static int removeFromKeystore(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        return SecurityECDH.removeFromKeystore(context, keystoreAlias);
    }

    public static boolean isValidDekuPublicKey(byte[] dekuPublicKey) {
        byte[] start = SecurityHandler.dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] end = SecurityHandler.dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8);

        int indexStart = Bytes.indexOf(dekuPublicKey, start);
        int indexEnd = Bytes.indexOf(dekuPublicKey, end);
//        Log.d(E2EEHandler.class.getName(), "Index start: " + indexStart);
//        Log.d(E2EEHandler.class.getName(), "Index end: " + indexEnd);
//        Log.d(E2EEHandler.class.getName(), "Data size: " + dekuPublicKey.length);

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
            String encodedText = text.substring(SecurityHandler.dekuTextStartPrefix.length(),
                    (text.length() - SecurityHandler.dekuTextEndPrefix.length()));
            return text.startsWith(SecurityHandler.dekuTextStartPrefix) &&
                    text.endsWith(SecurityHandler.dekuTextEndPrefix) &&
                    Helpers.isBase64Encoded(encodedText);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] buildDekuPublicKey(byte[] data) {
//        return SecurityHandler.convertPublicKeyToPEMFormat(data);
        return SecurityHandler.convertPublicKeyToDekuFormat(data);
    }

    public static byte[] extractTransmissionKey(byte[] data) {
        byte[] start = SecurityHandler.dekuHeaderStartPrefix.getBytes(StandardCharsets.UTF_8);
        byte[] end = SecurityHandler.dekuHeaderEndPrefix.getBytes(StandardCharsets.UTF_8);

        byte[] transmissionKey = new byte[data.length - (start.length + end.length)];
        System.arraycopy(data, start.length, transmissionKey, 0, transmissionKey.length);
        return transmissionKey;
    }

    public static byte[] buildForEncryptionRequest(Context context, String address) throws NumberParseException, GeneralSecurityException, IOException, InterruptedException {
        int session = 0;
        String keystoreAlias = getKeyStoreAlias(address, session);
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

    public static byte[] encryptText(Context context, String keystoreAlias, String text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        Log.d(E2EEHandler.class.getName(), "Encryption pub key: " +
                conversationsThreadsEncryption.getPublicKey());
        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));
        byte[] _secretKey = SecurityECDH.generateSecretKey(context, keystoreAlias, publicKey);
        Log.d(E2EEHandler.class.getName(), "Encrypt sharedsecret: " +
                Base64.encodeToString(_secretKey, Base64.DEFAULT));
        SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
        return SecurityAES.encryptAESGCM(text.getBytes(StandardCharsets.UTF_8), secretKey);
    }

    public static byte[] decryptText(Context context, String keystoreAlias, byte[] text) throws GeneralSecurityException, IOException, InterruptedException {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                ConversationsThreadsEncryption.getDao(context);
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        Log.d(E2EEHandler.class.getName(), "Decryption pub key: " +
                conversationsThreadsEncryption.getPublicKey());
        PublicKey publicKey = SecurityECDH.buildPublicKey(Base64.decode(
                conversationsThreadsEncryption.getPublicKey(), Base64.DEFAULT));
        byte[] _secretKey = SecurityECDH.generateSecretKey(context, keystoreAlias, publicKey);
        SecretKey secretKey = new SecretKeySpec(_secretKey, "AES");
        Log.d(E2EEHandler.class.getName(), "Decrypt sharedsecret: " +
                Base64.encodeToString(_secretKey, Base64.DEFAULT));
        return SecurityAES.decryptAESGCM(text, secretKey);
    }

    public static String buildTransmissionText(byte[] data) {
        return SecurityHandler.convertTextToDekuFormat(data);
    }

    public static byte[] extractTransmissionText(String text) {
        String encodedText = text.substring(SecurityHandler.dekuTextStartPrefix.length(),
                (text.length() - SecurityHandler.dekuTextEndPrefix.length()));

        return Base64.decode(encodedText, Base64.DEFAULT);
    }

    public static void clearAll(Context context) {
        CustomKeyStoreDao customKeyStoreDao = CustomKeyStore.getDao(context);
        customKeyStoreDao.clear();
    }
}
