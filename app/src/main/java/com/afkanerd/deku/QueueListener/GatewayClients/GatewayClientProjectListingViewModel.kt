package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore
import com.afkanerd.deku.Modules.ThreadingPoolExecutor

class GatewayClientProjectListingViewModel : ViewModel() {
    private lateinit var datastore: Datastore

    private var liveData : LiveData<GatewayClientProjects> = MutableLiveData()
    fun get(context: Context, gatewayClient: GatewayClient): LiveData<GatewayClientProjects>{
        datastore = Datastore.getDatastore(context)
        ThreadingPoolExecutor.executorService.execute {
            liveData = datastore.gatewayClientProjectDao().fetchLiveData(gatewayClient.id)
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
