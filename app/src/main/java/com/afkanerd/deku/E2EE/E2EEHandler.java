package com.afkanerd.deku.E2EE;


import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.Security.CustomKeyStore;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityECDH;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Headers;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Ratchets;
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.States;
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
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
                + Base64.encodeToString(data, Base64.NO_WRAP) +
                DEFAULT_TEXT_END_PREFIX_SHORTER;
    }

    public static byte[] convertPublicKeyToDekuFormat(byte[] data) {
        return Bytes.concat(DEFAULT_HEADER_START_PREFIX.getBytes(StandardCharsets.UTF_8),
                data, DEFAULT_HEADER_END_PREFIX.getBytes(StandardCharsets.UTF_8));
    }

    public static String deriveKeystoreAlias(String address, int mode) throws NumberParseException {
        String[] addressDetails = Helpers.getCountryNationalAndCountryCode(address);
        String keystoreAliasRequirements = addressDetails[0] + addressDetails[1] + "_" + mode;
        return Base64.encodeToString(keystoreAliasRequirements.getBytes(), Base64.NO_WRAP);
    }

    public static String getAddressFromKeystore(String keystoreAlias) {
        keystoreAlias = buildForOriginal(keystoreAlias);
        String decodedAlias = new String(Base64.decode(keystoreAlias, Base64.NO_WRAP),
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

    public static boolean isSelf(Context context, String keystoreAlias) throws UnrecoverableEntryException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                Datastore.getDatastore(context)
                        .conversationsThreadsEncryptionDao().fetch(keystoreAlias);

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAlias);

        if(conversationsThreadsEncryption == null || keyPair == null)
            return false;

        byte[] currentPubKey =
                Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.NO_WRAP);
        return Arrays.equals(currentPubKey, keyPair.getPublic().getEncoded());
    }

    public static boolean canCommunicateSecurely(Context context, String keystoreAlias, boolean strict) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if(strict)
            return isAvailableInKeystore(keystoreAlias) &&
                    Datastore.getDatastore(context).conversationsThreadsEncryptionDao()
                            .findByKeystoreAlias(keystoreAlias) != null;

        return (isAvailableInKeystore(keystoreAlias) &&
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao()
                        .findByKeystoreAlias(keystoreAlias) != null ||
                isAvailableInKeystore(keystoreAlias) &&
                        Datastore.getDatastore(context).conversationsThreadsEncryptionDao()
                                .findByKeystoreAlias(buildForSelf(keystoreAlias)) != null);
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
        customKeyStore.setPrivateKey(Base64.encodeToString(encryptedPrivateKey, Base64.NO_WRAP));
        customKeyStore.setPublicKey(Base64.encodeToString(keyPair.getPublic().getEncoded(),
                Base64.NO_WRAP));
        customKeyStore.setKeystoreAlias(keystoreAlias);

        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                CustomKeyStoreDao customKeyStoreDao = Datastore.getDatastore(context)
                        .customKeyStoreDao();
                customKeyStoreDao.insert(customKeyStore);
            }
        });
    }

    public static void removeFromKeystore(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        KeystoreHelpers.removeFromKeystore(context, keystoreAlias);
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                CustomKeyStoreDao customKeyStoreDao = Datastore.getDatastore(context)
                        .customKeyStoreDao();
                customKeyStoreDao.delete(keystoreAlias);
            }
        });
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
        if(text == null)
            return false;
        if(text.length() > (DEFAULT_TEXT_START_PREFIX_SHORTER.length() +
                DEFAULT_TEXT_END_PREFIX_SHORTER.length()) &&
                text.startsWith(DEFAULT_TEXT_START_PREFIX_SHORTER) &&
                text.endsWith(DEFAULT_TEXT_END_PREFIX_SHORTER))
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX_SHORTER.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX_SHORTER.length()));
        else if(text.length() > (DEFAULT_TEXT_START_PREFIX.length() +
                DEFAULT_TEXT_END_PREFIX.length()) &&
                text.startsWith(DEFAULT_TEXT_START_PREFIX) &&
                text.endsWith(DEFAULT_TEXT_END_PREFIX))
            encodedText = text.substring(DEFAULT_TEXT_START_PREFIX.length(),
                    (text.length() - DEFAULT_TEXT_END_PREFIX.length()));
        else return false;

        try {
            return Helpers.isBase64Encoded(encodedText);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] buildDefaultPublicKey(byte[] data) {
        return convertPublicKeyToDekuFormat(data);
    }

    public static byte[] extractTransmissionKey(byte[] data) {
        byte[] start = DEFAULT_HEADER_START_PREFIX.getBytes(StandardCharsets.UTF_8);
        byte[] end = DEFAULT_HEADER_END_PREFIX.getBytes(StandardCharsets.UTF_8);

        byte[] transmissionKey = new byte[data.length - (start.length + end.length)];
        System.arraycopy(data, start.length, transmissionKey, 0, transmissionKey.length);
        return transmissionKey;
    }

    /**
     * This uses session = 0, which is the default PublicKey values for.
     *
     * If the PublicKey in Keystore is the same as ConversationsEncryption database for the same alias,
     * this infers same person making a request and therefore uses the same public key.
     *
     * @param context
     * @param address
     * @return
     * @throws NumberParseException
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws InterruptedException
     */
    public static Pair<String, byte[]> buildForEncryptionRequest(Context context, String address,
                                                                 String keystoreAlias) throws Exception {
        int session = 0;

        if(keystoreAlias == null)
            keystoreAlias = deriveKeystoreAlias(address, session);
        PublicKey publicKey = createNewKeyPair(context, keystoreAlias);
        return new Pair<>(keystoreAlias, buildDefaultPublicKey(publicKey.getEncoded()));
    }

    public static String buildForSelf(String keystoreAlias) {
        return keystoreAlias + "_self";
    }

    public static String buildForOriginal(String keystoreAlias) {
        return keystoreAlias.endsWith("_self") ? keystoreAlias.split("_")[0] : keystoreAlias;
    }

    /**
     * Inserts the peer public key which would be used as the primary key for everything this peer.
     * @param context
     * @param publicKey
     * @param keystoreAlias
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws InterruptedException
     * @throws NumberParseException
     */
    public static long insertNewAgreementKeyDefault(Context context, byte[] publicKey, String keystoreAlias) throws GeneralSecurityException, IOException, InterruptedException, NumberParseException {
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                new ConversationsThreadsEncryption();
        conversationsThreadsEncryption.setPublicKey(Base64.encodeToString(publicKey, Base64.NO_WRAP));
        conversationsThreadsEncryption.setExchangeDate(System.currentTimeMillis());
        conversationsThreadsEncryption.setKeystoreAlias(keystoreAlias);

        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao();
        return conversationsThreadsEncryptionDao.insert(conversationsThreadsEncryption);
    }

    public static ConversationsThreadsEncryption fetchStoredPeerData(Context context,
                                                                     String keystoreAlias) {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao();
        return conversationsThreadsEncryptionDao.fetch(keystoreAlias);
    }

    /**
     * Get the default KeyPair, used in identifying the peers. This is different from the keypairs
     * stored during ratcheting.
     *
     * @param context
     * @param keystoreAlias
     * @return
     * @throws UnrecoverableEntryException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     */
    public static KeyPair getKeyPairBasedVersioning(Context context, String keystoreAlias) throws UnrecoverableEntryException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        final KeyPair[] keyPair = new KeyPair[1];
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomKeyStoreDao customKeyStoreDao = Datastore.getDatastore(context)
                            .customKeyStoreDao();
                    CustomKeyStore customKeyStore = customKeyStoreDao.find(keystoreAlias);
                    try {
                        if(customKeyStore != null)
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

    protected static String getKeystoreForRatchets(String keystoreAlias) {
        return keystoreAlias + "-ratchet-sessions";
    }

    /**
     * This returns a header, ciphertext and the mk.
     *
     * @param context
     * @param keystoreAlias
     * @param data
     * @return
     * @throws Throwable
     */
    public static byte[][] encrypt(Context context, final String keystoreAlias, byte[] data,
                                   boolean isSelf) throws Throwable {
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao();
        ConversationsThreadsEncryption conversationsThreadsEncryption = isSelf ?
                conversationsThreadsEncryptionDao.findByKeystoreAlias(buildForSelf(keystoreAlias)):
                conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);

        States states;
        final String keystoreAliasRatchet = getKeystoreForRatchets(keystoreAlias);

        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAliasRatchet);
        if(keyPair == null) {
            /**
             * You are Alice, so act like it
             */
            PublicKey publicKey = SecurityECDH.buildPublicKey(
                    Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.NO_WRAP));
            keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
            final byte[] SK = SecurityECDH.generateSecretKey(keyPair, publicKey);

            states = new States();
            byte[] output = Ratchets.ratchetInitAlice(keystoreAliasRatchet, states, SK, publicKey);

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                storeInCustomKeystore(context, keystoreAliasRatchet, states.DHs, output);
        }
        else {
            states = new States(keyPair, conversationsThreadsEncryption.getStates());
        }

        byte[] AD = Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.NO_WRAP);
        Pair<Headers, byte[][]> cipherPair = Ratchets.ratchetEncrypt(states, data, AD);

        conversationsThreadsEncryption.setStates(states.getSerializedStates());
        conversationsThreadsEncryptionDao.update(conversationsThreadsEncryption);

        return new byte[][]{Bytes.concat(cipherPair.first.getSerialized(), cipherPair.second[0]),
                cipherPair.second[1]};
    }

    public static byte[] decrypt(Context context, final String keystoreAlias, final byte[] cipherText,
                                 byte[] mk, byte[] _AD, boolean isSelf) throws Throwable {
        if(isSelf && !keystoreAlias.endsWith("_self"))
            throw new Exception("Expected " + keystoreAlias + "_self but got " + keystoreAlias);

        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao();
        ConversationsThreadsEncryption conversationsThreadsEncryption =
                conversationsThreadsEncryptionDao.findByKeystoreAlias(isSelf ?
                        buildForOriginal(keystoreAlias) :
                        keystoreAlias);

        String keystoreAliasRatchet = getKeystoreForRatchets(keystoreAlias);

        States states;
        Headers header = new Headers();

        byte[] outputCipherText = header.deSerializeHeader(cipherText);
        KeyPair keyPair = getKeyPairBasedVersioning(context, keystoreAliasRatchet);
        if(keyPair == null) {
            /**
             * You are Bob, act like it
             */
            keyPair = getKeyPairBasedVersioning(context, keystoreAlias);
            PublicKey publicKey = SecurityECDH.buildPublicKey(
                    Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.NO_WRAP));
            final byte[] SK = SecurityECDH.generateSecretKey(keyPair, publicKey);

            states = Ratchets.ratchetInitBob(new States(), SK, keyPair);
        } else {
            Log.d(E2EEHandler.class.getName(), "Yep not null no more...");
            states = new States(keyPair, conversationsThreadsEncryption.getStates());
            Log.d(E2EEHandler.class.getName(), states.getSerializedStates());
        }

        byte[] AD = _AD == null ?
                getKeyPairBasedVersioning(context, keystoreAlias).getPublic().getEncoded() : _AD;
        Pair<byte[], byte[]> decryptedText = Ratchets.ratchetDecrypt(keystoreAliasRatchet, states,
                header, outputCipherText, AD, mk);

        if(mk == null) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S && decryptedText.second != null) {
                storeInCustomKeystore(context, keystoreAliasRatchet, states.DHs, decryptedText.second);
            }
            conversationsThreadsEncryption.setStates(states.getSerializedStates());
            conversationsThreadsEncryptionDao.update(conversationsThreadsEncryption);
        }

        return decryptedText.first;
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

        return Base64.decode(encodedText, Base64.NO_WRAP);
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
                    Base64.encodeToString(publicKey, Base64.NO_WRAP))) {
                return IGNORE_KEY;
            }
        }
        return REQUEST_KEY;
    }

    public static void clear(Context context, String keystoreAlias) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        removeFromKeystore(context, keystoreAlias);
        removeFromKeystore(context, buildForSelf(keystoreAlias));
        removeFromKeystore(context, getKeystoreForRatchets(keystoreAlias));
        removeFromKeystore(context, getKeystoreForRatchets(buildForSelf(keystoreAlias)));
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                Datastore.getDatastore(context).conversationsThreadsEncryptionDao();
        conversationsThreadsEncryptionDao.delete(keystoreAlias);
        conversationsThreadsEncryptionDao.delete(buildForSelf(keystoreAlias));
        conversationsThreadsEncryptionDao.delete(getKeystoreForRatchets(keystoreAlias));
    }

}
