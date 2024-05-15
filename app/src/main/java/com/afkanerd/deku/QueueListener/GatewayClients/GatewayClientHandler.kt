package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context
import android.util.Log
import androidx.startup.AppInitializer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.Commons.Helpers
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager
import com.afkanerd.deku.WorkManagerInitializer
import java.util.concurrent.TimeUnit

class GatewayClientHandler(context: Context?) {
    var databaseConnector: Datastore = Datastore.getDatastore(context)

    @Throws(InterruptedException::class)
    fun add(gatewayClient: GatewayClient): Long {
        gatewayClient.date = System.currentTimeMillis()
        val id = longArrayOf(-1)
        val thread = Thread {
            val gatewayClientDAO = databaseConnector.gatewayClientDAO()
            id[0] = gatewayClientDAO.insert(gatewayClient)
        }
        thread.start()
        thread.join()

        return id[0]
    }

    @Throws(InterruptedException::class)
    fun delete(gatewayClient: GatewayClient) {
        gatewayClient.date = System.currentTimeMillis()
        val thread = Thread {
            val gatewayClientDAO = databaseConnector.gatewayClientDAO()
            gatewayClientDAO.delete(gatewayClient)
        }
        thread.start()
        thread.join()
    }

    @Throws(InterruptedException::class)
    fun update(gatewayClient: GatewayClient) {
        gatewayClient.date = System.currentTimeMillis()
        val thread = Thread {
            val gatewayClientDAO = databaseConnector.gatewayClientDAO()
            gatewayClientDAO.update(gatewayClient)
        }
        thread.start()
        thread.join()
    }

    @Throws(InterruptedException::class)
    fun fetch(id: Long): GatewayClient {
        val gatewayClient = arrayOf(GatewayClient())
        val thread = Thread {
            val gatewayClientDAO = databaseConnector.gatewayClientDAO()
            gatewayClient[0] = gatewayClientDAO.fetch(id)
        }
        thread.start()
        thread.join()

        return gatewayClient[0]
    }

    companion object {
        const val MIGRATIONS: String = "MIGRATIONS"
        const val MIGRATIONS_TO_11: String = "MIGRATIONS_TO_11"

        fun getPublisherDetails(context: Context?, projectName: String): List<String> {
            val simcards = SIMHandler.getSimCardInformation(context)

            val operatorCountry = Helpers.getUserCountry(context)

            val operatorDetails: MutableList<String> = ArrayList()
            for (i in simcards.indices) {
                val mcc = simcards[i].mcc.toString()
                val _mnc = simcards[i].mnc
                val mnc = if (_mnc < 10) "0$_mnc" else _mnc.toString()
                val carrierId = mcc + mnc

                val publisherName = "$projectName.$operatorCountry.$carrierId"
                operatorDetails.add(publisherName)
            }

            return operatorDetails
        }

        fun startListening(context: Context, gatewayClient: GatewayClient) {
            ThreadingPoolExecutor.executorService.execute(object : Runnable {
                override fun run() {
                    Datastore.getDatastore(context).gatewayClientDAO().update(gatewayClient)
                    Log.d(javaClass.name, "Gateway client: " + gatewayClient.activated)
                    if (gatewayClient.activated)
                        startWorkManager(context)
                }
            })
        }

        fun startWorkManager(context: Context) : WorkManager {
            val constraints : Constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
//            .setRequiresBatteryNotLow(true)
                    .build();

            val workManager = WorkManager.getInstance(context)

            ThreadingPoolExecutor.executorService.execute {
                Datastore.getDatastore(context).gatewayClientDAO().all.forEach {
                    Log.d(javaClass.name, "WorkManager: ${it.id}:${it.hostUrl}")
                    if(it.state == GatewayClient.STATE_INITIALIZING) {
                        it.state = GatewayClient.STATE_DISCONNECTED
                        Datastore.getDatastore(context).gatewayClientDAO().update(it)
                    }
                    if(it.activated) {
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
    }
}
