package com.afkanerd.deku.QueueListener.RMQ

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity

class RMQConnectionService : Service() {
    private var nConnected = 0
    private var nReconnecting = 0

    private lateinit var gatewayClientListLiveData: LiveData<List<GatewayClient>>

    private lateinit var workManagerLiveData: LiveData<List<WorkInfo>>

    private val gatewayClientObserver = Observer<List<GatewayClient>> {
        it.forEach {gatewayClient ->
            if(gatewayClient.activated)
                GatewayClientHandler.startWorkManager(applicationContext, gatewayClient)
        }
    }

    private val workManagerObserver = Observer<List<WorkInfo>> {
        it.forEach { workInfo ->
            when(workInfo.state) {
                WorkInfo.State.ENQUEUED -> { }
                WorkInfo.State.RUNNING -> {
                    ++nReconnecting
                    nConnected = if(nConnected <= 0) 0 else nConnected -1
                }
                WorkInfo.State.SUCCEEDED -> {
                    nReconnecting = if(nReconnecting <= 0) 0 else nReconnecting -1
                    ++nConnected
                }
                WorkInfo.State.FAILED -> {}
                WorkInfo.State.BLOCKED -> {}
                WorkInfo.State.CANCELLED -> {}
            }
        }
        createForegroundNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        workManagerLiveData.removeObserver(workManagerObserver)
        gatewayClientListLiveData.removeObserver(gatewayClientObserver)
    }

    override fun onCreate() {
        super.onCreate()

        workManagerLiveData = WorkManager.getInstance(applicationContext)
                .getWorkInfosByTagLiveData(GatewayClient::class.java.name)

        workManagerLiveData.observeForever(workManagerObserver)

        gatewayClientListLiveData = Datastore.getDatastore(applicationContext)
                .gatewayClientDAO().fetch()
        gatewayClientListLiveData.observeForever(gatewayClientObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createForegroundNotification()
        return START_STICKY
    }

    private fun createForegroundNotification() {
        val notificationIntent = Intent(applicationContext, GatewayClientListingActivity::class.java)
        val pendingIntent = PendingIntent
                .getActivity(applicationContext,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE)

        val description = "$nConnected ${getString(R.string.gateway_client_running_description)}\n" +
                "$nReconnecting ${getString(R.string.gateway_client_reconnecting_description)}"

        val notification =
                NotificationCompat.Builder(applicationContext,
                        getString(R.string.running_gateway_clients_channel_id))
                        .setContentTitle(getString(R.string.gateway_client_running_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setSilent(true)
                        .setOngoing(true)
                        .setContentText(description)
                        .setContentIntent(pendingIntent)
                        .build()

        val notificationId = 1234
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else startForeground(notificationId, notification)
    }

}