package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment
import com.afkanerd.deku.DefaultSMS.R

class GatewayClientListingActivity : AppCompatActivity() {

    var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gateway_client_listing)

        toolbar = findViewById(R.id.gateway_client_listing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.title = getString(R.string.gateway_client_listing_toolbar_title)

        val gatewayClientListingFragment = GatewayClientListingFragment()
        supportFragmentManager.beginTransaction()
                .add( R.id.view_fragment, gatewayClientListingFragment)
                .setReorderingAllowed(true)
                .setReorderingAllowed(true)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gateway_client_listing_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.gateway_client_add_manually) {
            val addGatewayIntent = Intent(applicationContext, GatewayClientAddActivity::class.java)
            startActivity(addGatewayIntent)
            return true
        }
        return false
    }

    companion object {
        var GATEWAY_CLIENT_ID: String = "GATEWAY_CLIENT_ID"
        var GATEWAY_CLIENT_ID_NEW: String = "GATEWAY_CLIENT_ID_NEW"
        var GATEWAY_CLIENT_USERNAME: String = "GATEWAY_CLIENT_USERNAME"
        var GATEWAY_CLIENT_PASSWORD: String = "GATEWAY_CLIENT_PASSWORD"
        var GATEWAY_CLIENT_VIRTUAL_HOST: String = "GATEWAY_CLIENT_VIRTUAL_HOST"
        var GATEWAY_CLIENT_HOST: String = "GATEWAY_CLIENT_HOST"
        var GATEWAY_CLIENT_PORT: String = "GATEWAY_CLIENT_PORT"
        var GATEWAY_CLIENT_FRIENDLY_NAME: String = "GATEWAY_CLIENT_FRIENDLY_NAME"

        var GATEWAY_CLIENT_LISTENERS: String = "GATEWAY_CLIENT_LISTENERS"
        var GATEWAY_CLIENT_STOP_LISTENERS: String = "GATEWAY_CLIENT_STOP_LISTENERS"
    }
}