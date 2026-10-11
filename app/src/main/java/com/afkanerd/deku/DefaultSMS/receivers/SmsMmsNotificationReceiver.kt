package com.afkanerd.deku.DefaultSMS.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.afkanerd.deku.DefaultSMS.ui.viewModels.SecureConversationViewModel
import com.afkanerd.deku.MainActivity
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.afkanerd.lib_smsmms_android.R
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.EncryptionController
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SavedEncryptedModes
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.getEncryptionModeStatesSync
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.removeEncryptionModeStates
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.removeEncryptionRatchetStates
import com.afkanerd.smswithoutborders_libsmsmms.data.data.models.SmsManager
import com.afkanerd.smswithoutborders_libsmsmms.data.entities.Conversations
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.NotificationTxType
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.getDatabase
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.notify
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.sendNotificationBroadcast
import com.afkanerd.smswithoutborders_libsmsmms.receivers.SmsMmsActionsImpl
import com.afkanerd.smswithoutborders_libsmsmms.receivers.SmsTextReceivedReceiver
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsMmsNotificationReceiver: BroadcastReceiver() {
    private val cls = MainActivity::class.java
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            SmsTextReceivedReceiver.SMS_SENT_BROADCAST_INTENT_LIB -> {
                val id = intent.getLongExtra("id", -1)
                val self = intent.getBooleanExtra("self", false)
                val type = intent.getStringExtra("type")
                CoroutineScope(Dispatchers.IO).launch {
                    context?.getDatabase()?.conversationsDao()
                        ?.getConversation(id)?.let { conversation ->
                            when(conversation.sms?.status) {
                                Telephony.Sms.STATUS_FAILED -> {
                                    notifyMessageFailedToSend(context, conversation)

                                    if(conversation.sms_data != null) {
                                        context.removeEncryptionModeStates(
                                            conversation.sms?.address!!)
                                    }
                                }
                                else -> {
                                    if(type == NotificationTxType.DATA.name) {
                                        processEncryptedContent(context, conversation)
                                    } else {
                                        val body = processEncryptedMessage(context, conversation)
                                        body?.let {
                                            conversation.sms?.body = it
                                            context.getDatabase().conversationsDao()
                                                ?.update(conversation)

                                        }
                                    }

                                    GatewayServerViewModel().route(context, conversation)

                                    context.notify(
                                        conversation = conversation,
                                        cls = cls,
                                        self = self,
                                    )
                                }
                            }
                        }
                }
            }

            SmsMmsActionsImpl.NOTIFICATION_REPLY_ACTION_INTENT_ACTION_REPLAY -> {
                val address = intent.getStringExtra("address")
                val threadId = intent.getIntExtra("threadId", -1)
                val subscriptionId = intent.getLongExtra("subscriptionId", -1)
                val reply = intent.getStringExtra("reply")

                val smsManager = SmsManager(SecureConversationViewModel())
                try {
                    smsManager.sendSms(
                        context = context!!,
                        text = reply!!,
                        address = address!!,
                        threadId = threadId,
                        subscriptionId = subscriptionId,
                        data = null,
                    ) { conversation ->
                        if(conversation == null) return@sendSms
                        context.sendNotificationBroadcast(
                            conversation, self=true, type = NotificationTxType.TEXT)
                    }
                } catch(e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun notifyMessageFailedToSend(context: Context, conversation: Conversations) {
        val content = context
            .getString(
                R.string
                    .message_failed_send_notification_description_a_message_failed_to_send_to) +
                " ${conversation.sms?.address}"

        context.notify(
            conversation = conversation,
            actions = false,
            text = content,
            cls = cls,
        )
    }

    private suspend fun processEncryptedContent(
        context: Context,
        conversation: Conversations
    ) {
        val data = conversation.sms_data!!
        try {
            if(EncryptionController.MessageRequestType.fromMessage(data) ==
                EncryptionController.MessageRequestType.TYPE_REQUEST
            ) {
                context.removeEncryptionRatchetStates(conversation.sms?.address!!)
                context.removeEncryptionModeStates(conversation.sms?.address!!)
            }

            EncryptionController.receiveRequest(
                context,
                conversation.sms?.address!!,
                data
            )
        } catch(e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun processEncryptedMessage(
        context: Context,
        conversation: Conversations,
    ) : String? {
        context.getEncryptionModeStatesSync(conversation.sms?.address!!)?.let { data ->
            val saveData = Gson().fromJson(data, SavedEncryptedModes::class.java)
            if(saveData.mode != EncryptionController.SecureRequestMode.REQUEST_ACCEPTED)
                return null

            return try {
                EncryptionController.decrypt(
                    context,
                    conversation.sms?.address!!,
                    conversation.sms?.body!!
                )
            } catch(e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
                null
            }
        }
        return null
    }
}