package com.afkanerd.deku.QueueListener.RMQ;

import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.ChannelN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RMQConnection {
    final boolean autoDelete = false;
    final boolean exclusive = false;
    final boolean durable = true;
    final boolean autoAck = false;

    public static final String MESSAGE_BODY_KEY = "text";
    public static final String MESSAGE_MSISDN_KEY = "to";
    public static final String MESSAGE_GLOBAL_MESSAGE_ID_KEY = "id";
    public static final String MESSAGE_SID = "sid";

    public Connection connection;

//    private Channel channel1;
//    private Channel channel2;

//    public Channel getChannel2() {
//        return channel2;
//    }
//
//    public void setChannel2(Channel channel2) {
//        this.channel2 = channel2;
//    }
//

    private boolean reconnecting = false;

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

//    private DeliverCallback deliverCallback, deliverCallback2;

    public RMQConnection(Connection connection) throws IOException {
        this.connection = connection;
    }

    public RMQConnection(){
    }

//    public Channel[] getChannels() throws IOException {
//        Channel channel1 = this.connection.createChannel();
//        Channel channel2 = this.connection.createChannel();
//
//        int prefetchCount = 1;
//        channel1.basicQos(prefetchCount);
//        channel2.basicQos(prefetchCount);
//
//        return new Channel[]{channel1, channel2};
//    }

//    public Channel[] setConnection(Connection connection) throws IOException {
//        this.connection = connection;
//
//        Channel channel1 = this.connection.createChannel();
//        channel1.basicRecover(true);
//        Channel channel2 = this.connection.createChannel();
//        channel2.basicRecover(true);
//
//        int prefetchCount = 1;
//        channel1.basicQos(prefetchCount);
//        channel2.basicQos(prefetchCount);
//
//        return new Channel[]{channel1, channel2};
//    }

    List<Channel> channelList = new ArrayList<>();
    public Channel createChannel() throws IOException {
        int prefetchCount = 1;
        Channel channel = this.connection.createChannel();
        channel.basicQos(prefetchCount);
        channelList.add(channel);
        return channelList.get(channelList.size() -1);
    }
    public void close() throws IOException {
        if(connection != null)
            connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public String createQueue(String exchangeName, String bindingKey, Channel channel) throws IOException {
        final String queueName = bindingKey.replaceAll("\\.", "_");

        channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        channel.queueBind(queueName, exchangeName, bindingKey);

        return queueName;
    }

//    public void createQueue1(String exchangeName, String bindingKey, DeliverCallback deliverCallback) throws IOException {
//        this.queueName = bindingKey.replaceAll("\\.", "_");
//        this.deliverCallback = deliverCallback;
//
//        this.channel1.queueDeclare(queueName, durable, exclusive, autoDelete, null);
//        this.channel1.queueBind(queueName, exchangeName, bindingKey);
//        this.channel1.addShutdownListener(new ShutdownListener() {
//            @Override
//            public void shutdownCompleted(ShutdownSignalException cause) {
//                Log.d(getClass().getName(), "CHannel shutdown listener called: " + cause.toString());
//            }
//        });
//    }
//
//    public void createQueue2(String exchangeName, String bindingKey, DeliverCallback deliverCallback) throws IOException {
//        this.queueName2 = bindingKey.replaceAll("\\.", "_");
//        this.deliverCallback = deliverCallback;
//
//        this.channel2.queueDeclare(queueName2, durable, exclusive, autoDelete, null);
//        this.channel2.queueBind(queueName2, exchangeName, bindingKey);
//    }

    public String consume(Channel channel, String queueName, DeliverCallback deliverCallback) throws IOException {
        /**
         * - Binding information dumb:
         * 1. .usd. = <anything>.usd.</anything>
         * 2. *.usd = <single anything>.usd
         * 3. #.usd = <many anything>.usd
         * 4. Can all be used in combination with each
         * 5. We can translate this into managing multiple service providers
         */

//        ShutdownListener shutdownListener2 = new ShutdownListener() {
//            @Override
//            public void shutdownCompleted(ShutdownSignalException cause) {
//                Log.d(getClass().getName(), "Channel shutdown listener called: " + cause.toString());
//                if(!cause.isInitiatedByApplication() && connection.isOpen()) {
//                    try {
//                        channels[1].basicConsume(queueName2, autoAck, deliverCallback, consumerTag -> {});
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
        return channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {});
    }

//    public void consume1() throws IOException {
//        /**
//         * - Binding information dumb:
//         * 1. .usd. = <anything>.usd.</anything>
//         * 2. *.usd = <single anything>.usd
//         * 3. #.usd = <many anything>.usd
//         * 4. Can all be used in combination with each
//         * 5. We can translate this into managing multiple service providers
//         */
//        this.channel1.basicConsume(this.queueName, autoAck, deliverCallback, consumerTag -> {});
//    }
//
//    public void consume2() throws IOException {
//        this.channel2.basicConsume(this.queueName2, autoAck, deliverCallback, consumerTag -> {});
//    }
}
