package com.afkanerd.deku.Router.Models

import android.database.Cursor
import android.util.Pair
import androidx.recyclerview.widget.DiffUtil
import androidx.work.DelegatingWorkerFactory
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.Router.GatewayServers.GatewayServer
import com.google.gson.annotations.Expose
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RouterItem(val conversation: Conversation) : Conversation(conversation) {
    var routingUniqueId: String? = null
    var url: String? = null
    var routingStatus: String? = null

    fun setConversationTag(tag: String) {
        conversation.tag = tag
    }

    fun serializeJson() : String {
        val json = Json {
            prettyPrint = true
        }
        return json.encodeToString(conversation)
    }

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
