package com.afkanerd.deku.DefaultSMS

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Base64
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Ratchets
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.States
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class CustomAppCompactActivity : DualSIMConversationActivity() {
    protected var address: String? = null
    protected var contactName: String? = null
    protected var threadId: String? = null
    protected var conversationsViewModel: ConversationsViewModel? = null

    protected var threadedConversationsViewModel: ThreadedConversationsViewModel? = null

    var databaseConnector: Datastore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!_checkIsDefaultApp()) {
            startActivity(Intent(this, DefaultCheckActivity::class.java))
            finish()
        }

        databaseConnector = Datastore.getDatastore(applicationContext)
    }

    private fun _checkIsDefaultApp(): Boolean {
        val myPackageName = packageName
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(this)

        return myPackageName == defaultPackage
    }

    protected open fun informSecured(secured: Boolean) {}

    protected fun sendTextMessage(text: String?, subscriptionId: Int, messageId: String?) {
        var messageId = messageId
        if (text != null) {
            if (messageId == null) messageId = System.currentTimeMillis().toString()

            val conversation = Conversation()
            conversation.text = text
            val messageIdFinal: String = messageId
            conversation.message_id = messageId
            conversation.thread_id = threadId
            conversation.subscription_id = subscriptionId
            conversation.type = Telephony.Sms.MESSAGE_TYPE_OUTBOX
            conversation.date = System.currentTimeMillis().toString()
            conversation.address = address
            conversation.status = Telephony.Sms.STATUS_PENDING

            if (conversationsViewModel != null) {
                CoroutineScope(Dispatchers.Default).launch{
                    try {
                        conversationsViewModel!!.insert(applicationContext, conversation)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@launch
                    }

                    if(E2EEHandler.isSecured(applicationContext, address!!)) {
                        val peerPublicKey = Base64.decode(E2EEHandler.secureFetchPeerPublicKey(
                            applicationContext, address!!,
                            E2EEHandler.isSelf(applicationContext, address!!)), Base64.DEFAULT)
                        var states = E2EEHandler.fetchStates(applicationContext, address!!)
                        if(states.isBlank()) {
                            val aliceState = States()
                            val SK = E2EEHandler.calculateSharedSecret(applicationContext, address!!,
                                peerPublicKey)
                            Ratchets.ratchetInitAlice(aliceState, SK, peerPublicKey)
                            states = aliceState.serializedStates
                        }
                        val sendingState = States(states)
                        val headerCipherText = Ratchets.ratchetEncrypt(sendingState,
                            conversation.text!!.encodeToByteArray(), peerPublicKey)
                        val msg = E2EEHandler.formatMessage(headerCipherText.first,
                            headerCipherText.second)
                        conversation.text = Base64.encodeToString(msg, Base64.DEFAULT)
                        E2EEHandler.storeState(applicationContext, sendingState.serializedStates,
                            address!!)
                    }
                    sendSMS(conversation)
                }
            }
        }
    }

    private fun sendSMS(conversation: Conversation) {
        try {
            SMSDatabaseWrapper.send_text(applicationContext, conversation, null)
        } catch (e: Exception) {
            e.printStackTrace()
            NativeSMSDB.Outgoing.register_failed( applicationContext, conversation.message_id,
                1 )
            conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
            conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
            conversation.error_code = 1
            conversationsViewModel!!.update(conversation)
        }
    }

    @Throws(InterruptedException::class)
    protected fun saveDraft(messageId: String?, text: String?) {
        if (text != null) {
            if (conversationsViewModel != null) {
                ThreadingPoolExecutor.executorService.execute {
                    val conversation = Conversation()
                    conversation.message_id = messageId
                    conversation.thread_id = threadId
                    conversation.text = text
                    conversation.isRead = true
                    conversation.type = Telephony.Sms.MESSAGE_TYPE_DRAFT
                    conversation.date = System.currentTimeMillis().toString()
                    conversation.address = address
                    conversation.status = Telephony.Sms.STATUS_PENDING
                    try {
                        conversationsViewModel!!.insert(applicationContext, conversation)
                        SMSDatabaseWrapper.saveDraft(applicationContext, conversation)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
