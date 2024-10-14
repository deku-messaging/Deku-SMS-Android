package com.afkanerd.deku.DefaultSMS.BroadcastReceivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Base64
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.BuildConfig
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.MagicNumber
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.extractPublicKeyFromPayload
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.getRequestType
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.sameRequest
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.isSecured
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.isValidPublicKey
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.makeSelfRequest
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler.secureStorePeerPublicKey
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

//import org.bouncycastle.operator.OperatorCreationException;
class IncomingDataSMSBroadcastReceiver : BroadcastReceiver() {
    var databaseConnector: Datastore? = null

    override fun onReceive(context: Context, intent: Intent) {
        /**
         * Important note: either image or dump it
         */

        databaseConnector = Datastore.getDatastore(context)

        if (intent.action == Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val regIncomingOutput =
                        NativeSMSDB.Incoming.register_incoming_data(context, intent)

                    val threadId = regIncomingOutput[NativeSMSDB.THREAD_ID]
                    val messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID]
                    val data = regIncomingOutput[NativeSMSDB.BODY]
                    val address = regIncomingOutput[NativeSMSDB.ADDRESS]
                    val strSubscriptionId = regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID]
                    val dateSent = regIncomingOutput[NativeSMSDB.DATE_SENT]
                    val date = regIncomingOutput[NativeSMSDB.DATE]
                    val subscriptionId = strSubscriptionId.toInt()

                    val byteData = Base64.decode(data, Base64.DEFAULT)
                    val isValidKey = isValidPublicKey(byteData)

                    val conversation = Conversation()
                    conversation.data = data
                    conversation.address = address
                    conversation.isIs_key = isValidKey
                    conversation.message_id = messageId
                    conversation.thread_id = threadId
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX
                    conversation.subscription_id = subscriptionId
                    conversation.date = dateSent
                    conversation.date = date

                    var isSelf = false
                    var isSecured = false
                    if (isValidKey) {
                        try {
                            val res = processAndGetFlags(context, byteData, address)
                            isSelf = res[0]
                            isSecured = res[1]
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    conversation.isIs_key = true
                    conversation.isIs_encrypted = isSecured

                    val finalIsSelf = isSelf
                    CoroutineScope(Dispatchers.Default).launch {
                        val threadedConversations =
                            databaseConnector!!.threadedConversationsDao()
                                .insertThreadAndConversation(context, conversation)
                        threadedConversations.isSelf = finalIsSelf
                        databaseConnector!!.threadedConversationsDao()
                            .update(context, threadedConversations)

                        val broadcastIntent = Intent(DATA_DELIVER_ACTION)
                        broadcastIntent.putExtra(Conversation.ID, messageId)
                        broadcastIntent.putExtra(Conversation.THREAD_ID, threadId)
                        context.sendBroadcast(broadcastIntent)
                        if (!threadedConversations.isIs_mute) NotificationsHandler.sendIncomingTextMessageNotification(
                            context,
                            conversation
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun processAndGetFlags(
        context: Context,
        data: ByteArray,
        address: String
    ): BooleanArray {
        /**
         * 0 - is Self
         * 1 - is conversation now encrypted (either request post agree or receiving agree)
         */
        var isSelf = false
        var isSecured = false

        val magicNumber = getRequestType(data)
        if (magicNumber != null) {
            when (magicNumber) {
                MagicNumber.REQUEST -> {
                    val publicKey = extractPublicKeyFromPayload(data)
                    if (sameRequest(context, address, publicKey)) {
                        makeSelfRequest(context, address)
                        isSelf = true
                    }
                    else if(E2EEHandler.containsPeer(context, address)) {
                        E2EEHandler.clear(context, address)
                    }
                    secureStorePeerPublicKey(
                        context, address,
                        extractPublicKeyFromPayload(data), false
                    )
                    isSecured = isSecured(context, address)
                }

                MagicNumber.ACCEPT -> {
                    secureStorePeerPublicKey(
                        context, address,
                        extractPublicKeyFromPayload(data), false
                    )
                    isSecured = isSecured(context, address)
                }

                MagicNumber.MESSAGE -> {}
            }
        }
        return booleanArrayOf(isSelf, isSecured)
    }

    companion object {
        var DATA_DELIVER_ACTION: String = BuildConfig.APPLICATION_ID + ".DATA_DELIVER_ACTION"

        var DATA_SENT_BROADCAST_INTENT: String =
            BuildConfig.APPLICATION_ID + ".DATA_SENT_BROADCAST_INTENT"

        var DATA_DELIVERED_BROADCAST_INTENT: String =
            BuildConfig.APPLICATION_ID + ".DATA_DELIVERED_BROADCAST_INTENT"
    }
}
