package com.example.swob_deku.Models.RMQ;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.example.swob_deku.Models.SIMHandler;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
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

    private String queueName, queueName2;

    private Connection connection;

    private Channel channel1;

    public Channel getChannel2() {
        return channel2;
    }

    public void setChannel2(Channel channel2) {
        this.channel2 = channel2;
    }

    private Channel channel2;

    private boolean reconnecting = false;

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

    private DeliverCallback deliverCallback, deliverCallback2;

    public RMQConnection(Connection connection) throws IOException {
        this.setConnection(connection);
    }

    public RMQConnection(){
    }

    public void setConnection(Connection connection) throws IOException {
        this.connection = connection;

        this.channel1 = this.connection.createChannel();
        this.channel2 = this.connection.createChannel();

        int prefetchCount = 1;
        this.channel1.basicQos(prefetchCount);
        this.channel2.basicQos(prefetchCount);
    }

    public void close() throws IOException {
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel1() {
        return channel1;
    }

    /**
     *
     * @param exchangeName
     * @param deliverCallback
     * @throws IOException
     */
    public void createQueue(String exchangeName, String bindingKey1, String bindingKey2, DeliverCallback deliverCallback, DeliverCallback deliverCallback2) throws IOException {
        this.queueName = bindingKey1.replaceAll("\\.", "_");
        this.deliverCallback = deliverCallback;

        ShutdownListener shutdownListener = new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                Log.d(getClass().getName(), "CHannel shutdown listener called: " + cause.toString());
                if(connection.isOpen()) {
                    try {
                        // Hopefully this triggers the reconnect mechanisms
                        connection.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        this.channel1.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        this.channel1.queueBind(queueName, exchangeName, bindingKey1);
        this.channel1.addShutdownListener(shutdownListener);

        if (bindingKey2 != null && deliverCallback2 != null) {
            this.queueName2 = bindingKey2.replaceAll("\\.", "_");
            this.deliverCallback2 = deliverCallback2;

            this.channel2.queueDeclare(queueName2, durable, exclusive, autoDelete, null);
            this.channel2.queueBind(queueName2, exchangeName, bindingKey2);
            this.channel2.addShutdownListener(shutdownListener);
        }
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

    public void consume() throws IOException {
        /**
         * - Binding information dumb:
         * 1. .usd. = <anything>.usd.</anything>
         * 2. *.usd = <single anything>.usd
         * 3. #.usd = <many anything>.usd
         * 4. Can all be used in combination with each
         * 5. We can translate this into managing multiple service providers
         */
        this.channel1.basicConsume(this.queueName, autoAck, deliverCallback, consumerTag -> {});
        if(this.channel2 != null)
            this.channel2.basicConsume(this.queueName2, autoAck, deliverCallback2, consumerTag -> {});
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
