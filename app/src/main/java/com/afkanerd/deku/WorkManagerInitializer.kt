package com.afkanerd.deku

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
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
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<Operation> {
    override fun create(context: Context): Operation {
        val constraints : Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .setRequiresBatteryNotLow(true)
            .build();

        val gatewayClientListenerWorker = OneTimeWorkRequestBuilder<RMQWorkManager>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(GatewayClient::class.simpleName!!)
            .build();

//        val configuration = Configuration.Builder().build()
//        WorkManager.initialize(context, configuration)

        Log.d(javaClass.name, "Enqueueing work for later")
        val workManager = WorkManager.getInstance(context)
        return workManager.enqueueUniqueWork(
            ThreadedConversationsActivity.UNIQUE_WORK_MANAGER_NAME,
            ExistingWorkPolicy.KEEP,
            gatewayClientListenerWorker
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java, NotificationsInitializer::class.java)
    }
}