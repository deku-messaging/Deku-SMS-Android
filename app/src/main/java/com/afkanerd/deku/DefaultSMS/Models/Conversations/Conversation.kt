package com.afkanerd.deku.DefaultSMS.Models.Conversations

import android.database.Cursor
import android.provider.Telephony
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Entity(indices = [Index(value = ["message_id"], unique = true)])
open class Conversation : Cloneable {
    @PrimaryKey(autoGenerate = true) @EncodeDefault var id: Long = 0
    @EncodeDefault var message_id: String? = null
    @EncodeDefault var thread_id: String? = null
    @EncodeDefault var date: String? = null
    @EncodeDefault var date_sent: String? = null
    @EncodeDefault var type = 0
    @EncodeDefault var num_segments = 0
    @EncodeDefault var subscription_id = 0
    @EncodeDefault var status = 0
    @EncodeDefault var error_code = 0
    @ColumnInfo(name = "read") @EncodeDefault var isRead = false

    @ColumnInfo(name = "is_encrypted")
    @EncodeDefault var isIs_encrypted = false

    @ColumnInfo(name = "is_key")
    @EncodeDefault var isIs_key = false

    @ColumnInfo(name = "is_image")
    @EncodeDefault var isIs_image = false

    @EncodeDefault var formatted_date: String? = null
    @EncodeDefault var address: String? = null
    @EncodeDefault var text: String? = null
    @EncodeDefault var data: String? = null

    @Transient @EncodeDefault var _mk: String? = null

    @Ignore()
    @EncodeDefault
    var tag: String? = null
    constructor()
    constructor(cursor: Cursor) {
        val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY)
        val threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID)
        val addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS)
        val dateIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE)
        val dateSentIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE_SENT)
        val typeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE)
        val statusIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.STATUS)
        val readIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.READ)
        val subscriptionIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID)

        message_id = cursor.getString(idIndex)
        text = cursor.getString(bodyIndex)
        thread_id = cursor.getString(threadIdIndex)
        address = cursor.getString(addressIndex)
        date = cursor.getString(dateIndex)
        date_sent = cursor.getString(dateSentIndex)
        type = cursor.getInt(typeIndex)
        status = cursor.getInt(statusIndex)
        isRead = cursor.getInt(readIndex) == 1
        subscription_id = cursor.getInt(subscriptionIdIndex)
    }

    constructor(conversation: Conversation) {
        message_id = conversation.message_id
        text = conversation.text
        thread_id = conversation.thread_id
        address = conversation.address
        date = conversation.date
        date_sent = conversation.date_sent
        type = conversation.type
        status = conversation.status
        isRead = conversation.isRead
        subscription_id = conversation.subscription_id
    }

//    override fun equals(obj: Any?): Boolean {
//        if (obj is Conversation) {
//            val conversation = obj
//            return conversation.thread_id == thread_id
//                    && conversation.message_id == message_id
//                    && conversation.text == text
//                    && conversation.data == data
//                    && conversation.date == date
//                    && conversation.address == address
//                    && conversation.status == status
//                    && conversation.isRead == isRead
//                    && conversation.type == type
//        }
//        return super.equals(obj)
//    }

    companion object {
        const val ID = "ID"
        const val ADDRESS = "ADDRESS"
        const val THREAD_ID = "THREAD_ID"
        const val SUBSCRIPTION_ID = "SUBSCRIPTION_ID"
        const val SHARED_SMS_BODY = "sms_body"
        fun build(cursor: Cursor): Conversation {
            return Conversation(cursor)
        }

        val DIFF_CALLBACK: DiffUtil.ItemCallback<Conversation> = object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
                return oldItem.message_id == newItem.message_id
            }

            override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
                return oldItem == newItem
            }
        }
    }
}
