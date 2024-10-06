package com.afkanerd.deku.DefaultSMS.Settings;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.LocaleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afkanerd.deku.DefaultSMS.Fragments.DevelopersFragment;
//import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
//import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

//        Preference securityPrivacyPreference = findPreference(getString(R.string.settings_security_end_to_end));
//        securityPrivacyPreference.setFragment(EndToEndFragments.class.getCanonicalName());

        Preference developersPreferences = findPreference(getString(R.string.settings_advanced_developers_key));
        developersPreferences.setFragment(DevelopersFragment.class.getCanonicalName());

        ListPreference languagePreference = findPreference(getString(R.string.settings_locale));
        languagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String languageLocale = (String) newValue;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                    getContext().getSystemService(LocaleManager.class)
                            .setApplicationLocales(
                                    new LocaleList(Locale.forLanguageTag(languageLocale)));
                }
                else {
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageLocale);
                    AppCompatDelegate.setApplicationLocales(appLocale);
                }
                return true;
            }
        });
    }
}