package com.afkanerd.deku

import android.content.Context
import androidx.startup.Initializer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val constraints : Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

        val gatewayClientListenerWorker : OneTimeWorkRequest = OneTimeWorkRequest
            .Builder(RMQWorkManager::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria( BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(GatewayClient::class.simpleName!!)
            .build();

        return WorkManager.getInstance(context);
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}