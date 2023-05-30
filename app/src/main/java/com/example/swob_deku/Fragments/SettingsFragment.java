package com.example.swob_deku.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.swob_deku.GatewayClientAddActivity;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.GatewayServerListingActivity;
import com.example.swob_deku.R;

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