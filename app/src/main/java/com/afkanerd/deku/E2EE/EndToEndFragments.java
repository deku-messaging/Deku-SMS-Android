package com.afkanerd.deku.E2EE;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.afkanerd.deku.DefaultSMS.R;

public class EndToEndFragments extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.communication_encryption_preferences, rootKey);
    }
}
