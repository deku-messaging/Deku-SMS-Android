package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.Modules.ThreadingPoolExecutor

class GatewayClientProjectListingViewModel : ViewModel() {
    private lateinit var datastore: Datastore

    private lateinit var liveData : LiveData<List<GatewayClientProjects>>
    fun get(context: Context, gatewayClientId: Long): LiveData<List<GatewayClientProjects>>{
        datastore = Datastore.getDatastore(context)
        if(!::liveData.isInitialized) {
            liveData = MutableLiveData()
            liveData = datastore.gatewayClientProjectDao().fetchGatewayClientId(gatewayClientId)
        }
        return liveData
    }

    fun insert(gatewayClientProjects: GatewayClientProjects) {
        datastore.gatewayClientProjectDao().insert(gatewayClientProjects)
    }

    fun update(gatewayClientProjects: GatewayClientProjects) {
        datastore.gatewayClientProjectDao().update(gatewayClientProjects)
    }
}
