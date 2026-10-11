package com.afkanerd.deku.Forwarder.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.Forwarder.data.models.FTP
import com.afkanerd.deku.Forwarder.data.models.RouterItem
import com.afkanerd.deku.Forwarder.data.models.SMTP
import com.afkanerd.deku.Forwarder.extensions.toSha256
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.getDatabase
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RouterWorkManager(context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {

    val gatewayClientsSettingsManager = GatewayClientsSettingsManager(applicationContext)

    override suspend fun doWork(): Result {
        val gatewayServerId = inputData.getLong(GATEWAY_SERVER_ID, -1)
        val conversationId = inputData.getString(CONVERSATION_ID)!!

        return withContext(Dispatchers.Default) {
            val datastore = Datastore.getDatastore(applicationContext)
            val gatewayServer = datastore.gatewayServerDAO()
                .get(gatewayServerId.toString()) ?: throw Exception("Gateway server not found!: $gatewayServerId")

            val conversation = applicationContext.getDatabase()
                .conversationsDao()
                ?.getConversation(conversationId.toLong()) ?: throw Exception("Conversation not found: $conversationId")

            val isHashIncoming = gatewayClientsSettingsManager.getHashIncomingAddress().first()
            val routerItem = RouterItem( conversation.sms!!, isHashIncoming)
            routerItem.tag = gatewayServer.tag

            val jsonStringBody = routerItem.serializeJson()
            println(jsonStringBody)

            when(gatewayServer.protocol) {
                SMTP.PROTOCOL -> {
                    try {
                        RouterHandler.routeSmtpMessages(jsonStringBody, gatewayServer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is MailConnectException) { Result.retry() }
                        return@withContext Result.failure()
                    }
                }
                FTP.PROTOCOL -> {
                    try {
                        RouterHandler.routeFTPMessages(jsonStringBody, gatewayServer)
                    } catch (e: Exception) {
                        Log.e(javaClass.getName(), "Exception: ", e)
                        return@withContext Result.failure()
                    }
                }
                else -> {
                    try {
                        gatewayServer.URL ?: throw Exception("Gateway server URL not found")
                        val url = if(Network.isUrlAnIpAddress(gatewayServer.URL!!))
                            "http://${gatewayServer.URL}"
                        else gatewayServer.URL!!
                        Log.d(javaClass.name, "Forwarding to: $url")

                        val response = Network.jsonRequestPost(url, jsonStringBody)
                        Log.d(javaClass.name, "Forwarding response code: ${response.response.statusCode}")
                        when(response.response.statusCode) {
                            in 500..600 -> return@withContext Result.retry()
                            else -> return@withContext Result.failure()
                        }
                    } catch(e: Exception) {
                        Log.e(javaClass.name, "Exception routing", e)
                        return@withContext Result.retry()
                    }
                }
            }
            Result.success()
        }
    }

    companion object {
        var GATEWAY_SERVER_ID = "GATEWAY_SERVER_ID"
        var CONVERSATION_ID = "CONVERSATION_ID"
    }
}