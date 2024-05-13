package com.afkanerd.deku.QueueListener.RMQ

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SubscriptionInfo
import android.util.Log
import android.view.inputmethod.CorrectionInfo
import androidx.core.app.NotificationCompat
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.junit.Assert
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

class RMQConnectionService : Service() {
    private lateinit var databaseConnector: Datastore
    private lateinit var subscriptionInfoList: List<SubscriptionInfo>
    private val resourceSemaphore = Semaphore(1)
    private lateinit var messageStateChangedBroadcast: BroadcastReceiver
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startAllGatewayClientConnections()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(javaClass.name, "Ending connection...")
        unregisterReceiver(messageStateChangedBroadcast)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        databaseConnector = Datastore.getDatastore(applicationContext)
        subscriptionInfoList = SIMHandler.getSimCardInformation(applicationContext)
        handleBroadcast()
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
                        val rmqID = intent.getLongExtra(RMQConnection.RMQ_ID, -1)

                        Assert.assertTrue(!consumerTag.isNullOrEmpty())
                        Assert.assertTrue(deliveryTag != -1L)
                        Assert.assertTrue(rmqID != -1L)

                        rmqConnectionList.forEach {rmq ->
                            if(rmq.id == rmqID) {
                                rmq.findChannelByTag(consumerTag!!)?.let {
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
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            registerReceiver(messageStateChangedBroadcast, intentFilter, RECEIVER_EXPORTED)
        else registerReceiver(messageStateChangedBroadcast, intentFilter)
    }

    @Serializable
    private data class SMSRequest(val text: String, val to: String, val sid: String, val id: String)
    private suspend fun sendSMS(smsRequest: SMSRequest,
                                subscriptionId: Int,
                                consumerTag: String,
                                deliveryTag: Long,
                                rmqConnectionId: Long) {
        resourceSemaphore.acquire()
        val messageId = System.currentTimeMillis()
        resourceSemaphore.release()

        val threadId = Telephony.Threads.getOrCreateThreadId(applicationContext, smsRequest.to)

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
                .insertThreadAndConversation(applicationContext, conversation)
        SMSDatabaseWrapper.send_text(applicationContext, conversation, bundle)
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

    private fun startAllGatewayClientConnections() {
        Log.d(javaClass.name, "Starting all connections...")
        createForegroundNotification()

        ThreadingPoolExecutor.executorService.execute {
            databaseConnector.gatewayClientDAO().all.forEach {
                connectGatewayClient(it)
            }
        }
    }


    private val rmqConnectionList = ArrayList<RMQConnection>()
    private fun startConnection(factory: ConnectionFactory, gatewayClient: GatewayClient) {
        Log.d(javaClass.name, "Starting new connection...")
        gatewayClient.state = GatewayClient.STATE_RECONNECTING
        databaseConnector.gatewayClientDAO().update(gatewayClient)

        // TODO: use this without internet connection and catch that error and best handle
        try {
            val connection = factory.newConnection(ThreadingPoolExecutor.executorService,
                    gatewayClient.friendlyConnectionName)
            gatewayClient.state = GatewayClient.STATE_CONNECTED
            databaseConnector.gatewayClientDAO().update(gatewayClient)

            val rmqConnection = RMQConnection(gatewayClient.id, connection)
            rmqConnectionList.add(rmqConnection)

            connection.addShutdownListener {
                /**
                 * The logic here, if the user has not deactivated this - which can be known
                 * from the database connection state then reconnect this client
                 */
                Log.e(javaClass.name, "Connection shutdown cause: $it")
                if(databaseConnector.gatewayClientDAO().fetch(gatewayClient.id)
                                .activated) {
                    rmqConnectionList.remove(rmqConnection)
                    createForegroundNotification()
                    startConnection(factory, gatewayClient)
                }
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

            createForegroundNotification()
        } catch (e: Exception) {
            when(e) {
                is TimeoutException -> {
                    e.printStackTrace()
                    Thread.sleep(3000)
                    Log.d(javaClass.name, "Attempting a reconnect to the server...")
                    startConnection(factory, gatewayClient)
                }
                is IOException -> {
                    Log.e(javaClass.name, "IO Exception connecting rmq", e)
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
                        println("Some error occuring with design")
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

    private fun connectGatewayClient(gatewayClient: GatewayClient) {
        Log.d(javaClass.name, "Starting new service connection...")

        val factory = ConnectionFactory()
        factory.username = gatewayClient.username
        factory.password = gatewayClient.password
        factory.virtualHost = gatewayClient.virtualHost
        factory.host = gatewayClient.hostUrl
        factory.port = gatewayClient.port
        factory.isAutomaticRecoveryEnabled = true
        factory.exceptionHandler = DefaultExceptionHandler()

        factory.setRecoveryDelayHandler {
            Log.w(javaClass.name, "Factory recovering...: $it")
            createForegroundNotification()
            10000
        }

//        factory.setTrafficListener(object : TrafficListener {
//            override fun write(outboundCommand: Command) {
//            }
//
//            override fun read(inboundCommand: Command) {
//                if (disconnected) {
//                    Objects.requireNonNull(connectionList[gatewayClient.id]).abort()
//                    connectionList.remove(gatewayClient.id)
//                    startAllGatewayClientConnections()
//                    disconnected = false
//                }
//            }
//        })
        startConnection(factory, gatewayClient)
    }


    private fun stop(gatewayClientId: Long) {
        val iterator = rmqConnectionList.iterator()
        while(iterator.hasNext()) {
            val gatewayClient = iterator.next()
            if(gatewayClient.id == gatewayClientId) {
                rmqConnectionList.remove(gatewayClient)
                break
            }
        }

        if (rmqConnectionList.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            createForegroundNotification()
        }
    }

    private fun createForegroundNotification() {
        val notificationIntent = Intent(applicationContext, GatewayClientListingActivity::class.java)
        val pendingIntent = PendingIntent
                .getActivity(applicationContext,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE)

        var description = "N/A ${getString(R.string.gateway_client_running_description)}"

//        if (reconnecting > 0)
//            description += "N/A ${getString(R.string.gateway_client_reconnecting_description)}"

        val notification =
                NotificationCompat.Builder(applicationContext,
                        getString(R.string.running_gateway_clients_channel_id))
                        .setContentTitle(applicationContext
                                .getString(R.string.gateway_client_running_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setSilent(true)
                        .setOngoing(true)
                        .setContentText(description)
                        .setContentIntent(pendingIntent)
                        .build()

        val NOTIFICATION_ID: Int = 1234
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else startForeground(NOTIFICATION_ID, notification)
    }
}
