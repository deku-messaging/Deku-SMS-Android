package com.afkanerd.deku.E2EE;


import android.content.Context;
import android.util.Base64;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;

public class E2EEHandler {

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }
    public static String getKeyStoreAlias(String address, int sessionNumber) throws NumberParseException {
        String[] addressDetails = Helpers.getCountryNationalAndCountryCode(address);
        String keystoreAliasRequirements = addressDetails[0] + addressDetails[1] + "_" + sessionNumber;
        return Base64.encodeToString(keystoreAliasRequirements.getBytes(), Base64.NO_WRAP);
    }

    public static boolean isAvailableInKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        /*
         * Load the Android KeyStore instance using the
         * AndroidKeyStore provider to list the currently stored entries.
         */
        SecurityECDH securityECDH = new SecurityECDH();
        return securityECDH.isAvailableInKeystore(keystoreAlias);
    }

    public static PublicKey createNewKeyPair(Context context, String keystoreAlias)
            throws GeneralSecurityException, InterruptedException {
        return SecurityECDH.generateKeyPair(context, keystoreAlias);
    }

    public static void removeFromKeystore(String keystoreAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        SecurityECDH securityECDH = new SecurityECDH();
        securityECDH.removeFromKeystore(keystoreAlias);
    }
}
