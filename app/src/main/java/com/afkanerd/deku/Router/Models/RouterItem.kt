package com.afkanerd.deku.Router.Models

import android.database.Cursor
import android.util.Pair
import androidx.recyclerview.widget.DiffUtil
import androidx.work.DelegatingWorkerFactory
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.Router.GatewayServers.GatewayServer
import kotlinx.serialization.Serializable


@Serializable
data class RouterItem(val conversation: Conversation) : Conversation(conversation) {
    var routingUniqueId: String? = null
    var url: String? = null
    var tag: String? = null
    val MSISDN: String = ADDRESS
    var routingDate: Long = 0
    var routingStatus: String? = null
    var sid: String? = null
    var reportedStatus: String? = null
//    var text = getText()

//    override fun equals(other: Any?): Boolean {
//        if (other is RouterItem) {
//            val conversation = other as Conversation
//            return super.equals(conversation) && routingStatus == other.routingStatus
//        }
//        return false
//    }

    companion object {
        fun build(cursor: Cursor?): RouterItem {
            return cursor?.let { Conversation.build(it) } as RouterItem
        }
        class DIFF_CALLBACK : DiffUtil.ItemCallback<Pair<RouterItem, GatewayServer>>() {
            override fun areItemsTheSame(oldItem: Pair<RouterItem, GatewayServer>,
                                         newItem: Pair<RouterItem, GatewayServer>): Boolean {
                // Logic to compare if two items represent the same data (e.g., by ID)
                return oldItem.first.routingUniqueId == newItem.first.routingUniqueId
            }

            override fun areContentsTheSame(oldItem: Pair<RouterItem, GatewayServer>,
                                         newItem: Pair<RouterItem, GatewayServer>): Boolean {
                // Logic to compare if content of two items are the same
                return oldItem == newItem
            }
        }
    }
}
