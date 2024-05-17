package com.afkanerd.deku.QueueListener.RMQ

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DeliverCallback
import okhttp3.internal.toImmutableMap
import java.io.IOException

class RMQConnection(var id: Long, var connection: Connection) {
    private val autoDelete: Boolean = false
    private val exclusive: Boolean = false
    private val durable: Boolean = true

    private val channelList: MutableList<Channel> = ArrayList()
    private val channelTagMap = mutableMapOf<String, Channel>()


    fun removeChannel(channel: Channel) {
        channelList.remove(channel)
    }

    fun createChannel(): Channel {
        return connection.createChannel().apply {
            val prefetchCount = 1
            basicQos(prefetchCount)
        }
    }

    fun bindChannelToTag(channel: Channel, channelTag: String)  {
        channelTagMap[channelTag] = channel
    }

    fun findChannelByTag(channelTag: String) : Channel? {
        return channelTagMap[channelTag]
    }

    fun close() {
        if (connection.isOpen)
            connection.close()
    }

    fun createQueue(exchangeName: String, bindingKey: String, channel: Channel,
                    queueName: String = bindingKey.replace("\\.".toRegex(), "_")) :
            String {
        channel.queueDeclare(queueName, durable, exclusive, autoDelete, null)
        channel.queueBind(queueName, exchangeName, bindingKey)

        return queueName
    }

    companion object {
        const val MESSAGE_SID: String = "sid"

        const val RMQ_ID: String = "RMQ_ID"
        const val RMQ_DELIVERY_TAG: String = "RMQ_DELIVERY_TAG"
        const val RMQ_CONSUMER_TAG: String = "RMQ_CONSUMER_TAG"
    }
}
