package com.afkanerd.deku.Forwarder.ui.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.RemoteListeners.extensions.isBase64Encoded
import com.afkanerd.deku.Forwarder.data.RouterHandler
import com.afkanerd.deku.Forwarder.data.RouterHandler.getTagForGatewayServers
import com.afkanerd.deku.Forwarder.data.RouterHandler.getTagForMessages
import com.afkanerd.deku.Forwarder.data.GatewayClientsSettingsManager
import com.afkanerd.deku.Forwarder.data.RouterWorkManager
import com.afkanerd.deku.Forwarder.data.RouterWorkManager.Companion.CONVERSATION_ID
import com.afkanerd.deku.Forwarder.data.RouterWorkManager.Companion.GATEWAY_SERVER_ID
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import com.afkanerd.smswithoutborders_libsmsmms.data.entities.Conversations
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.getDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class RoutedConversationsItems(
    var conversation: Conversations,
    var workInfo: WorkInfo,
    var gatewayServerId: String,
)
class GatewayServerViewModel : ViewModel() {
    private val _workFlowItems = MutableStateFlow<List<RoutedConversationsItems>>(emptyList()) // default
    val workFlowItems: StateFlow<List<RoutedConversationsItems>> = _workFlowItems.asStateFlow()

    fun fetch(context: Context): Flow<List<GatewayServer>> {
        return Datastore.getDatastore(context).gatewayServerDAO().all
    }

    fun getActiveWorkManagerItems(
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(RouterHandler.TAG_NAME_GATEWAY_SERVER)
                .collect { workInfos ->
                    val routedConversationsItems = mutableListOf<RoutedConversationsItems>()
                    workInfos.forEach { workInfo ->
                        val workInfoPair = RouterHandler.workInfoParser(workInfo)
                        val messageId = workInfoPair.first
                        val gatewayServerId = workInfoPair.second

                        val conversation = context.getDatabase().conversationsDao()
                            ?.getConversation(messageId.toLong())
                        routedConversationsItems.add(RoutedConversationsItems(
                            conversation = conversation!!,
                            workInfo = workInfo,
                            gatewayServerId = gatewayServerId
                        ))
                    }
                    _workFlowItems.value = routedConversationsItems
                }
        }
    }

    suspend fun route(
        context: Context,
        conversation: Conversations
    ) {
        val gatewayClientsSettingsManager = GatewayClientsSettingsManager(context)
        val routeOnLowBattery = gatewayClientsSettingsManager.getRouteOnLowBattery().first()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(!routeOnLowBattery)
            .build()

        val isBase64 = conversation.sms!!.body!!.isBase64Encoded()
        val gatewayServerList = Datastore.getDatastore(context.applicationContext)
            .gatewayServerDAO()
            .allList  ?: throw Exception("Failed to fetch gateway servers")

        for (gatewayServer in gatewayServerList) {
            if (gatewayServer.format != null &&
                gatewayServer.format == GatewayServer.BASE64_FORMAT && !isBase64
            ) continue

            try {
                val routeMessageWorkRequest =
                    OneTimeWorkRequest.Builder(RouterWorkManager::class.java)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            WorkRequest.Companion.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS
                        )
                        .addTag(RouterHandler.TAG_NAME_GATEWAY_SERVER)
                        .addTag(getTagForMessages(conversation.id.toString()))
                        .addTag(getTagForGatewayServers(gatewayServer.id))
                        .setInputData(
                            Data.Builder()
                                .putLong(
                                    GATEWAY_SERVER_ID,
                                    gatewayServer.id
                                )
                                .putString(
                                    CONVERSATION_ID,
                                    conversation.id.toString()
                                )
                                .build()
                        )
                        .build()

                val uniqueWorkName: String = conversation.id.toString() + ":" +
                        gatewayServer.URL + ":" + gatewayServer.protocol
                val workManager = WorkManager.getInstance(context)
                val operation = workManager.enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.KEEP,
                    routeMessageWorkRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun update(
        context: Context,
        gatewayClient: GatewayServer,
        completeCallback: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            Datastore.getDatastore(context).gatewayServerDAO().update(gatewayClient)
            completeCallback()
        }
    }

    fun add(
        context: Context,
        gatewayClient: GatewayServer,
        completeCallback: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            Datastore.getDatastore(context).gatewayServerDAO().insert(gatewayClient)
            completeCallback()
        }
    }

    fun delete(
        context: Context,
        gatewayClient: GatewayServer,
        completeCallback: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            Datastore.getDatastore(context).gatewayServerDAO().delete(gatewayClient)
            completeCallback()
        }
    }

    fun setRouteOnLowBattery(context: Context, route: Boolean) {
        viewModelScope.launch {
            val gatewayClientsSettingsManager = GatewayClientsSettingsManager(context)
            gatewayClientsSettingsManager.setRouteOnLowBattery(route)
        }
    }

    fun setClearRouteCache(context: Context, clear: Boolean) {
        viewModelScope.launch {
            val gatewayClientsSettingsManager = GatewayClientsSettingsManager(context)
            gatewayClientsSettingsManager.setClearRouteCache(clear)
        }
    }

    fun setHashIncomingAddress(context: Context, clear: Boolean) {
        viewModelScope.launch {
            val gatewayClientsSettingsManager = GatewayClientsSettingsManager(context)
            gatewayClientsSettingsManager.setHashIncomingAddress(clear)
        }
    }

    fun clearAllRouted(context: Context) {
        WorkManager.getInstance(context).pruneWork()
    }
}