package com.afkanerd.deku.Router.Settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.afkanerd.deku.DefaultSMS.R

class GatewayServerSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gateway_server_settings_preferences, rootKey)
    }
}