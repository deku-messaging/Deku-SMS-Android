package com.afkanerd.deku.DefaultSMS.Models;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class SettingsHandler {
    public static boolean alertNotEncryptedCommunicationDisabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("encryption_disable", false);
    }

}
