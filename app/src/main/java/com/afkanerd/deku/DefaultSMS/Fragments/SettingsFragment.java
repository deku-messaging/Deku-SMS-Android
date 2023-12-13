package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afkanerd.deku.E2EE.EndToEndFragments;
//import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
//import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        Preference securityPrivacyPreference = findPreference("settings_security_end_to_end");
        securityPrivacyPreference.setFragment(EndToEndFragments.class.getCanonicalName());

        Preference developersPreferences = findPreference(getString(R.string.settings_advanced_developers_key));
        developersPreferences.setFragment(DevelopersFragment.class.getCanonicalName());
    }
}