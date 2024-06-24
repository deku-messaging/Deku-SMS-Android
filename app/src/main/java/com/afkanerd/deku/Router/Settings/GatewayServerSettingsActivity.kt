package com.afkanerd.deku.Router.Settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.afkanerd.deku.DefaultSMS.R

class GatewayServerSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gateway_server_settings)

        val myToolbar = findViewById<View>(R.id.gateway_server_toolbar) as Toolbar
        setSupportActionBar(myToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.title = getString(R.string.gateway_server_settings_title)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.gateway_server_settings_fragment_container,
                        GatewayServerSettingsFragment())
                .commit()
    }
}