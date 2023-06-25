package com.example.swob_deku.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class SettingsHandler {
    public static boolean checkEncryptedMessagingDisabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("encryption_disable", false);
    }

}
