package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afkanerd.deku.E2EE.EndToEndFragments;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;
import com.afkanerd.deku.DefaultSMS.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        Preference securityPrivacyPreference = findPreference("security_id_end_to_end_encryption");
        securityPrivacyPreference.setFragment(EndToEndFragments.class.getCanonicalName());

        Preference smsRoutingSMSWithoutBorders = findPreference("settings_sms_routing_gateway_clients");
        smsRoutingSMSWithoutBorders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(getContext(), GatewayServerListingActivity.class));
                return true;
            }
        });

        Preference smsListeningSMSWithoutBorders = findPreference("settings_sms_listening_gateway_clients");
        smsListeningSMSWithoutBorders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(getContext(), GatewayClientListingActivity.class));
                return true;
            }
        });
    }
}