package com.afkanerd.deku.DefaultSMS.ui.viewModels

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.afkanerd.deku.rust.RustyRatchetBridge
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.EncryptionController
import com.afkanerd.lib_smsmms_android.R
import com.afkanerd.smswithoutborders_libsmsmms.data.data.models.SmsManager
import com.afkanerd.smswithoutborders_libsmsmms.data.entities.Conversations
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.getDatabase
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.sendSms
import com.afkanerd.smswithoutborders_libsmsmms.ui.viewModels.ConversationsViewModel
import com.afkanerd.smswithoutborders_libsmsmms.ui.viewModels.CustomsConversationsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecureConversationViewModel: CustomsConversationsViewModel() {
    var mode by mutableStateOf(EncryptionController.SecureRequestMode.REQUEST_NONE)

    override fun sendSms(
        context: Context,
        text: String,
        address: String,
        subscriptionId: Long,
        threadId: Int,
        data: ByteArray?,
        bundle: Bundle,
        callback: (Conversations?) -> Unit
    ) {
        viewModelScope.launch {
            if(mode == EncryptionController.SecureRequestMode.REQUEST_ACCEPTED) {
                withContext(Dispatchers.Default) {
                    val cipherText = EncryptionController.encrypt(
                        context = context,
                        address = address,
                        text = text
                    )
                    if(cipherText.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(com.afkanerd.deku.DefaultSMS.R.string.empty_encrypted_cipher_something_went_wrong),
                                Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }
                    super.sendSms(
                        context = context,
                        text = cipherText,
                        address = address,
                        subscriptionId = subscriptionId,
                        threadId = threadId,
                        data = data,
                        bundle = bundle
                    ) { conversation ->
                        if(conversation == null) return@sendSms

                        conversation.sms?.body = text
                        context.getDatabase().conversationsDao()?.update(conversation)

                        callback(conversation)
                    }
                }
            }
            else {
                super.sendSms(
                    context,
                    text,
                    address,
                    subscriptionId,
                    threadId,
                    data,
                    bundle,
                    callback
                )
            }
        }
    }

    fun requestSecureConversation(
        context: Context,
        address: String,
        subscriptionId: Long,
        threadId: Int,
        callback: (Conversations) -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val publicKey = EncryptionController.sendRequest(context, address, mode)

                try {
                    val smsManager = SmsManager(ConversationsViewModel())
                    smsManager.sendSms(
                        text = "",
                        address = address,
                        threadId = threadId,
                        subscriptionId = subscriptionId,
                        data = publicKey,
                        context = context,
                    ) { conversation ->
                        conversation?.let { callback(it) }
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context,
                            context.getString(R.string.something_went_wrong_with_sending),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}