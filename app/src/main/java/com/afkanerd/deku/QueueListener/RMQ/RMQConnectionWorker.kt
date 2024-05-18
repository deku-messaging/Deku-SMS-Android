package com.afkanerd.deku.QueueListener.RMQ

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SubscriptionInfo
import android.util.Log
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper
import com.afkanerd.deku.Modules.SemaphoreManager
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjects
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.ConsumerShutdownSignalCallback
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.ShutdownSignalException
import com.rabbitmq.client.impl.DefaultExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.anyOf
import org.junit.Assert
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

class RMQConnectionWorker(val context: Context, val gatewayClientId: Long) {
    private lateinit var rmqConnection: RMQConnection
    private val factory = ConnectionFactory()

    private val databaseConnector: Datastore = Datastore.getDatastore(context)
    private val subscriptionInfoList: List<SubscriptionInfo> =
            SIMHandler.getSimCardInformation(context)

    private lateinit var messageStateChangedBroadcast: BroadcastReceiver

    init {
        handleBroadcast()
    }
    fun start() {
        connectGatewayClient(gatewayClientId)
    }

    private fun handleBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT)
        messageStateChangedBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != null && intentFilter.hasAction(intent.action)) {
                    Log.d(javaClass.name, "Received incoming broadcast")
                    if (intent.hasExtra(RMQConnection.MESSAGE_SID) &&
                            intent.hasExtra(RMQConnection.RMQ_DELIVERY_TAG)) {

                        val sid = intent.getStringExtra(RMQConnection.MESSAGE_SID)
                        val messageId = intent.getStringExtra(NativeSMSDB.ID)

                        val consumerTag = intent.getStringExtra(RMQConnection.RMQ_CONSUMER_TAG)
                        val deliveryTag =
                                intent.getLongExtra(RMQConnection.RMQ_DELIVERY_TAG, -1)

                        Assert.assertTrue(!consumerTag.isNullOrEmpty())
                        Assert.assertTrue(deliveryTag != -1L)

                        rmqConnection.findChannelByTag(consumerTag!!)?.let {
                            Log.d(javaClass.name, "Received an ACK of the message...")
                            if (resultCode == Activity.RESULT_OK) {
                                ThreadingPoolExecutor.executorService.execute {
                                    if (it.isOpen) it.basicAck(deliveryTag, false)
                                }
                            } else {
                                Log.w(javaClass.name, "Rejecting message sent")
                                ThreadingPoolExecutor.executorService.execute {
                                    if (it.isOpen) it.basicReject(deliveryTag, true)
                                }
                            }
                        }

                    }
                }
            }
        }

        context.registerReceiver(messageStateChangedBroadcast, intentFilter,
                Context.RECEIVER_EXPORTED)
    }

    @Serializable
    private data class SMSRequest(val text: String, val to: String, val sid: String, val id: Int)
    private suspend fun sendSMS(smsRequest: SMSRequest,
                                subscriptionId: Int,
                                consumerTag: String,
                                deliveryTag: Long,
                                rmqConnectionId: Long) {
        SemaphoreManager.resourceSemaphore.acquire()
        val messageId = System.currentTimeMillis()
        SemaphoreManager.resourceSemaphore.release()

        val threadId = Telephony.Threads.getOrCreateThreadId(context, smsRequest.to)

        val bundle = Bundle()
        bundle.putString(RMQConnection.MESSAGE_SID, smsRequest.sid)
        bundle.putString(RMQConnection.RMQ_CONSUMER_TAG, consumerTag)
        bundle.putLong(RMQConnection.RMQ_DELIVERY_TAG, deliveryTag)
        bundle.putLong(RMQConnection.RMQ_ID, rmqConnectionId)

        val conversation = Conversation()
        conversation.message_id = messageId.toString()
        conversation.text = smsRequest.text
        conversation.address = smsRequest.to
        conversation.subscription_id = subscriptionId
        conversation.type = Telephony.Sms.MESSAGE_TYPE_OUTBOX
        conversation.date = System.currentTimeMillis().toString()
        conversation.thread_id = threadId.toString()
        conversation.status = Telephony.Sms.STATUS_PENDING

        databaseConnector.threadedConversationsDao()
                .insertThreadAndConversation(context, conversation)
        SMSDatabaseWrapper.send_text(context, conversation, bundle)
        Log.d(javaClass.name, "SMS sent...")
    }
    private fun getDeliverCallback(channel: Channel, subscriptionId: Int,
                                   rmqConnectionId: Long): DeliverCallback {
        return DeliverCallback { consumerTag: String, delivery: Delivery ->
            val message = String(delivery.body, StandardCharsets.UTF_8)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val smsRequest = Json.decodeFromString<SMSRequest>(message)
                    sendSMS(smsRequest,
                            subscriptionId,
                            consumerTag,
                            delivery.envelope.deliveryTag,
                            rmqConnectionId)
                } catch (e: Exception) {
                    Log.e(javaClass.name, "", e)
                    when(e) {
                        is SerializationException -> {
                            channel.let {
                                if (it.isOpen)
                                    it.basicReject(delivery.envelope.deliveryTag, false)
                            }
                        }
                        is IllegalArgumentException -> {
                            channel.let {
                                if (it.isOpen)
                                    it.basicReject(delivery.envelope.deliveryTag, true)
                            }
                        }
                        else -> {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun startConnection(factory: ConnectionFactory, gatewayClient: GatewayClient) {
        Log.d(javaClass.name, "Starting new connection...")

        try {
            val connection = factory.newConnection(ThreadingPoolExecutor.executorService,
                    gatewayClient.friendlyConnectionName)

            rmqConnection = RMQConnection(gatewayClient.id, connection)

            connection.addShutdownListener {
                /**
                 * The logic here, if the user has not deactivated this - which can be known
                 * from the database connection state then reconnect this client.
                 */
                Log.e(javaClass.name, "Connection shutdown cause: $it")
                if(gatewayClient.activated)
                    GatewayClientHandler.startWorkManager(context, gatewayClient)
            }

            val gatewayClientProjectsList = databaseConnector.gatewayClientProjectDao()
                    .fetchGatewayClientIdList(gatewayClient.id)

            for (i in gatewayClientProjectsList.indices) {
                for (j in subscriptionInfoList.indices) {
                    val channel = rmqConnection.createChannel()
                    val gatewayClientProjects = gatewayClientProjectsList[i]
                    val subscriptionId = subscriptionInfoList[j].subscriptionId
                    val bindingName = if (j > 0)
                        gatewayClientProjects.binding2Name else gatewayClientProjects.binding1Name

                    Log.d(javaClass.name, "Starting channel for sim slot $j in project #$i")
                    startChannelConsumption(rmqConnection, channel, subscriptionId,
                            gatewayClientProjects, bindingName)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            when(e) {
                is TimeoutException, is IOException -> {
                    e.printStackTrace()
                    Thread.sleep(3000)
                    Log.d(javaClass.name, "Attempting a reconnect to the server...")
                    startConnection(factory, gatewayClient)
                }
                else -> {
                    Log.e(javaClass.name, "Exception connecting rmq", e)
                }
            }
        }
    }

    private fun startChannelConsumption(rmqConnection: RMQConnection,
                                channel: Channel,
                                subscriptionId: Int,
                                gatewayClientProjects: GatewayClientProjects,
                                bindingName: String) {
        Log.d(javaClass.name, "Starting channel connection")
        channel.basicRecover(true)
        val deliverCallback = getDeliverCallback(channel, subscriptionId, rmqConnection.id)
        val queueName = rmqConnection.createQueue(gatewayClientProjects.name, bindingName, channel)
        val messagesCount = channel.messageCount(queueName)

        val consumerTag = channel.basicConsume(queueName, false, deliverCallback,
                object : ConsumerShutdownSignalCallback {
                    override fun handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) {
                        if (rmqConnection.connection.isOpen) {
                            Log.e(javaClass.name, "Consumer error", sig)
                            startChannelConsumption(rmqConnection,
                                    rmqConnection.createChannel(),
                                    subscriptionId,
                                    gatewayClientProjects,
                                    bindingName)
                        }
                    }
                })
        Log.d(javaClass.name, "Created Queue: $queueName ($messagesCount) - tag: $consumerTag")
        rmqConnection.bindChannelToTag(channel, consumerTag)
    }


    private fun connectGatewayClient(gatewayClientId: Long) {
        Log.d(javaClass.name, "Starting new service connection...")

        ThreadingPoolExecutor.executorService.execute {
            val gatewayClient = Datastore.getDatastore(context).gatewayClientDAO()
                    .fetch(gatewayClientId)

            factory.username = gatewayClient.username
            factory.password = gatewayClient.password
            factory.virtualHost = gatewayClient.virtualHost
            factory.host = gatewayClient.hostUrl
            factory.port = gatewayClient.port
            factory.exceptionHandler = DefaultExceptionHandler()

            /**
             * Increase connectivity sensitivity
             */
            factory.isAutomaticRecoveryEnabled = false
            startConnection(factory, gatewayClient)
        }
    }

}
