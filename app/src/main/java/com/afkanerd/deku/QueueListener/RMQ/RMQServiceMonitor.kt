package com.afkanerd.deku.QueueListener.RMQ

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity
import com.afkanerd.deku.Router.Models.RouterHandler

object RMQServiceMonitor {

    var nConnected = 0
    var nReconnecting = 0

    private lateinit var workManagerLiveData: LiveData<List<WorkInfo>>
    fun monitorRMQConnections(context: Context) : LiveData<List<WorkInfo>> {
        if(!::workManagerLiveData.isInitialized) {
            val workManager = WorkManager.getInstance(context)
            workManagerLiveData =  workManager
                    .getWorkInfosByTagLiveData(GatewayClient::class.java.name)
        }
        return workManagerLiveData
    }

}