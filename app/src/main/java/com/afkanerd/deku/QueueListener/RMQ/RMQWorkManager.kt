package com.afkanerd.deku.QueueListener.RMQ

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.Companion.GATEWAY_CLIENT_LISTENERS


class RMQWorkManager(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val intent = Intent(applicationContext, RMQConnectionService::class.java)
        val sharedPreferences = applicationContext
                .getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE)
        if (sharedPreferences.all.isNotEmpty()) {
            try {
                applicationContext.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(javaClass.name, "Exception with starting RMQ services:", e)
                if (e is ForegroundServiceStartNotAllowedException) {
                    notifyUserToReconnectSMSServices()
                }
                return Result.failure()
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
