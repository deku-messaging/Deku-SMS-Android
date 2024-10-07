package com.afkanerd.deku.DefaultSMS

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper
import com.afkanerd.deku.Modules.ThreadingPoolExecutor

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
                ThreadingPoolExecutor.executorService.execute(Runnable {
                    try {
                        conversationsViewModel!!.insert(applicationContext, conversation)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@Runnable
                    }
                    try {
                        SMSDatabaseWrapper.send_text(applicationContext, conversation, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        NativeSMSDB.Outgoing.register_failed(
                            applicationContext,
                            messageIdFinal, 1
                        )
                        conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
                        conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
                        conversation.error_code = 1
                        conversationsViewModel!!.update(conversation)
                    }
                })
            }
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
