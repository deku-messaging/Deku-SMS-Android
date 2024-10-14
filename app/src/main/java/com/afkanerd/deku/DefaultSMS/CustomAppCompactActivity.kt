package com.afkanerd.deku.DefaultSMS

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import org.spongycastle.jcajce.provider.symmetric.ARC4.Base

open class CustomAppCompactActivity : DualSIMConversationActivity() {
    protected var address: String? = null
    protected var contactName: String? = null
    protected var threadId: String? = null
    protected var conversationsViewModel: ConversationsViewModel? = null

    protected var threadedConversationsViewModel: ThreadedConversationsViewModel? = null

    var databaseConnector: Datastore? = null

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!_checkIsDefaultApp()) {
            startActivity(Intent(this, DefaultCheckActivity::class.java))
            finish()
        }

        databaseConnector = Datastore.getDatastore(applicationContext)

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(applicationContext, "Request granted...",
                        Toast.LENGTH_LONG).show()
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            }
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

    private fun sendTxt(conversation: Conversation) {
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

    private fun sendSMS(conversation: Conversation) {

        when {
            ContextCompat.checkSelfPermission( applicationContext,
                android.Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED -> {
                sendTxt(conversation)
            }
//            ActivityCompat.shouldShowRequestPermissionRationale( this,
//                android.Manifest.permission.READ_PHONE_STATE) -> {
//                // In an educational UI, explain to the user why your app requires this
//                // permission for a specific feature to behave as expected, and what
//                // features are disabled if it's declined. In this UI, include a
//                // "cancel" or "no thanks" button that lets the user continue
//                // using your app without granting the permission.
//            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher?.launch(android.Manifest.permission.READ_PHONE_STATE)
            }
        }

//        try {
//            SMSDatabaseWrapper.send_text(applicationContext, conversation, null)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            NativeSMSDB.Outgoing.register_failed( applicationContext, conversation.message_id,
//                1 )
//            conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
//            conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
//            conversation.error_code = 1
//            conversationsViewModel!!.update(conversation)
//        }
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
