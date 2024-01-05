package com.afkanerd.smswithoutborders.libsignal_doubleratchet;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class KeystoreHelpers {

    public static boolean isAvailableInKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.containsAlias(keystoreAlias);
    }

    public static void removeAllFromKeystore(Context context) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Enumeration<String> aliases = keyStore.aliases();
        if(aliases.hasMoreElements())
            do {
                keyStore.deleteEntry(aliases.nextElement());
            } while(aliases.hasMoreElements());
    }

    public static void removeFromKeystore(Context context, String keystoreAlias) throws KeyStoreException,
            CertificateException, IOException, NoSuchAlgorithmException, InterruptedException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(keystoreAlias);
    }

    public static void storeInCustomKeyStore(String keystoreAlias, KeyPair keyPair) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.setKeyEntry(keystoreAlias, keyPair.getPrivate(), null, new Certificate[1]);
        keyStore.store(null);
    }

    public static KeyPair getKeyPairFromKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(keystoreAlias, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            PublicKey publicKey = keyStore.getCertificate(keystoreAlias).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        }
        return null;
    }
}
