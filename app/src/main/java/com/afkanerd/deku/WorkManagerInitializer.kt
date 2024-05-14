package com.afkanerd.deku

import android.content.Context
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
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val constraints : Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .setRequiresBatteryNotLow(true)
            .build();

        val workManager = WorkManager.getInstance(context)

        ThreadingPoolExecutor.executorService.execute {
            Datastore.getDatastore(context).gatewayClientDAO().all.forEach {
                if(it.activated) {
                    Log.d(javaClass.name, "WorkManager: ${it.id}:${it.hostUrl}")
                    val gatewayClientListenerWorker = OneTimeWorkRequestBuilder<RMQWorkManager>()
                            .setConstraints(constraints)
                            .setBackoffCriteria(
                                    BackoffPolicy.LINEAR,
                                    WorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS
                            )
                            .setInputData(Data.Builder()
                                    .putLong(GatewayClient.GATEWAY_CLIENT_ID, it.id)
                                    .build())
                            .addTag(GatewayClient::class.simpleName!!)
                            .build();

                    workManager.enqueueUniqueWork(
                            ThreadedConversationsActivity.UNIQUE_WORK_MANAGER_NAME,
                            ExistingWorkPolicy.KEEP,
                            gatewayClientListenerWorker
                    )
                }
            }
        }
        return workManager
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java, NotificationsInitializer::class.java)
    }
}