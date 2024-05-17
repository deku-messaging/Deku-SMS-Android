package com.afkanerd.deku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.startup.Initializer
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import androidx.work.WorkRequest
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler
import com.afkanerd.deku.QueueListener.RMQ.RMQConnectionService
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<Intent> {
    override fun create(context: Context): Intent {
        val intent = Intent(context, RMQConnectionService::class.java)

        context.startForegroundService(intent)

        return intent
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java, NotificationsInitializer::class.java)
    }
}