package com.afkanerd.deku.Router.Models

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.Modules.Network
import com.afkanerd.deku.Router.FTP
import com.afkanerd.deku.Router.SMTP
import com.sun.mail.util.MailConnectException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.mail.MessagingException
import javax.mail.SendFailedException

class RouterWorkManager (context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams) {
    override fun doWork(): Result {
        val gatewayServerId = inputData.getLong(GATEWAY_SERVER_ID, -1)
        val conversationId = inputData.getString(CONVERSATION_ID)

        val datastore = Datastore.getDatastore(applicationContext)
        val gatewayServer = datastore.gatewayServerDAO()[gatewayServerId.toString()]
        val conversation = datastore.conversationDao().getMessage(conversationId)

        val routerItem = RouterItem(conversation)
        routerItem.tag = gatewayServer.getTag()

        val jsonStringBody = routerItem.serializeJson()
        println(jsonStringBody)

        when(gatewayServer.getProtocol()) {
            SMTP.PROTOCOL -> {
                try {
                    RouterHandler.routeSmtpMessages(jsonStringBody, gatewayServer)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e is MailConnectException) { return Result.retry() }
                    return Result.failure()
                }
            }
            FTP.PROTOCOL -> {
                try {
                    RouterHandler.routeFTPMessages(jsonStringBody, gatewayServer)
                } catch (e: Exception) {
                    Log.e(javaClass.getName(), "Exception: ", e)
                    return Result.failure()
                }
            }
            else -> {
                try {
                    when(Network.jsonRequestPost(gatewayServer.url, jsonStringBody)
                            .response.statusCode) {
                        in 500..600 -> Result.retry()
                        else -> Result.failure()
                    }
                } catch(e: Exception) {
                    Log.e(javaClass.name, "Exception routing", e)
                    Result.retry()
                }
            }
        }

        return Result.success()
    }

    companion object {
        var GATEWAY_SERVER_ID = "GATEWAY_SERVER_ID"
        var CONVERSATION_ID = "CONVERSATION_ID"
    }
}
