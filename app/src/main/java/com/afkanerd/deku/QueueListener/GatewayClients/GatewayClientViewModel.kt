package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.Modules.ThreadingPoolExecutor

class GatewayClientViewModel : ViewModel() {
    private var gatewayClientList: LiveData<List<GatewayClient>> = MutableLiveData()

    private lateinit var datastore: Datastore

    fun getGatewayClientList(context: Context): LiveData<List<GatewayClient>> {
        datastore = Datastore.getDatastore(context)
        if(gatewayClientList.value.isNullOrEmpty()) {
            gatewayClientList = loadGatewayClients()
        }
        return gatewayClientList
    }

    private fun loadGatewayClients() : LiveData<List<GatewayClient>> {
        return datastore.gatewayClientDAO().fetch()
    }

    fun update(gatewayClient: GatewayClient) {
        ThreadingPoolExecutor.executorService.execute {
            datastore.gatewayClientDAO().update(gatewayClient)
        }
    }

//    private fun normalizeGatewayClients(gatewayClients: List<GatewayClient>): List<GatewayClient> {
//        val filteredGatewayClients: MutableList<GatewayClient> = ArrayList()
//        for (gatewayClient in gatewayClients) {
//            var contained = false
//            for (gatewayClient1 in filteredGatewayClients) {
//                if (gatewayClient1.same(gatewayClient)) {
//                    contained = true
//                    break
//                }
//            }
//            if (!contained) {
//                filteredGatewayClients.add(gatewayClient)
//            }
//        }
//
//        return filteredGatewayClients
//    }
}
