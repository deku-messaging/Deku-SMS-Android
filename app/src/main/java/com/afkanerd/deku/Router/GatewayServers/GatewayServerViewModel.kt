package com.afkanerd.deku.Router.GatewayServers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afkanerd.deku.Datastore

class GatewayServerViewModel : ViewModel() {
    private lateinit var gatewayServersList: LiveData<List<GatewayServer>>

    private lateinit var datastore: Datastore
    operator fun get(context: Context): LiveData<List<GatewayServer>> {
        if(!::gatewayServersList.isInitialized) {
            datastore = Datastore.getDatastore(context)
            gatewayServersList = MutableLiveData()
            loadGatewayServers()
        }
        return gatewayServersList
    }

    private fun loadGatewayServers() {
        gatewayServersList = datastore.gatewayServerDAO().all
    }
}
