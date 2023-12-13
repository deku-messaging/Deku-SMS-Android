package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;

import io.reactivex.rxjava3.annotations.NonNull;

public class DevelopersFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.developer_preferences, rootKey);

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
