package com.afkanerd.deku.QueueListener.RMQ

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.Companion.GATEWAY_CLIENT_LISTENERS
import org.junit.Assert


class RMQWorkManager(context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams) {
    override fun doWork(): Result {
        val rmqConnectionService = RMQConnectionService()

        val intent = Intent(applicationContext, rmqConnectionService.javaClass)

        val gatewayClientId = inputData.getLong(GatewayClient.GATEWAY_CLIENT_ID, -1)

        Assert.assertTrue(gatewayClientId.toInt() != -1)

        intent.putExtra(GatewayClient.GATEWAY_CLIENT_ID, gatewayClientId)

        try {
            applicationContext.startForegroundService(intent)
            Log.d(javaClass.name, "Started RMQConnection foreground service")
        } catch (e: Exception) {
            Log.e(javaClass.name, "Exception with starting RMQ services:", e)
            when(e) {
                is ForegroundServiceStartNotAllowedException -> {
                    notifyUserToReconnectSMSServices()
                    Result.retry()
                }
                else -> {
                    Result.failure()
                }
            }
        }
        return Result.success()
    }

    private fun notifyUserToReconnectSMSServices() {
        val NOTIFICATION_ID: Int = 12345
        val notificationIntent = Intent(applicationContext, ThreadedConversationsActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(applicationContext, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE)

        val notification =
                NotificationCompat.Builder(applicationContext,
                        applicationContext.getString(R.string.foreground_service_failed_channel_id))
                        .setContentTitle(applicationContext
                                .getString(R.string.foreground_service_failed_channel_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentText(applicationContext
                                .getString(R.string.foreground_service_failed_channel_description))
                        .setContentIntent(pendingIntent)
                        .build()

        val notificationManager =
                NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
