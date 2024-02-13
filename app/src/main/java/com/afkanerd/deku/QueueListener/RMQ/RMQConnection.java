package com.afkanerd.deku.QueueListener.RMQ;

import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;

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
        this.setConnection(connection);
    }

    public RMQConnection(){
    }

    public Channel[] getChannels() throws IOException {
        Channel channel1 = this.connection.createChannel();
        Channel channel2 = this.connection.createChannel();

        int prefetchCount = 1;
        channel1.basicQos(prefetchCount);
        channel2.basicQos(prefetchCount);

        return new Channel[]{channel1, channel2};
    }

    public Channel[] setConnection(Connection connection) throws IOException {
        this.connection = connection;

        Channel channel1 = this.connection.createChannel();
        channel1.basicRecover(true);
        Channel channel2 = this.connection.createChannel();
        channel2.basicRecover(true);

        int prefetchCount = 1;
        channel1.basicQos(prefetchCount);
        channel2.basicQos(prefetchCount);

        return new Channel[]{channel1, channel2};
    }

    public void close() throws IOException {
        if(connection != null)
            connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     *
     * @param exchangeName
     * @param deliverCallback
     * @throws IOException
     */
    public String[] createQueue(String exchangeName, String bindingKey1, String bindingKey2,
                                Channel[] channels) throws IOException {
        String queueName = bindingKey1.replaceAll("\\.", "_");
        String queueName2 = null;

        channels[0].queueDeclare(queueName, durable, exclusive, autoDelete, null);
        channels[0].queueBind(queueName, exchangeName, bindingKey1);

        if (bindingKey2 != null && channels.length > 1) {
            queueName2 = bindingKey2.replaceAll("\\.", "_");

            channels[1].queueDeclare(queueName2, durable, exclusive, autoDelete, null);
            channels[1].queueBind(queueName2, exchangeName, bindingKey2);
        }

        return new String[]{queueName, queueName2};
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

    public String[] consume(Channel[] channels, String queueName, String queueName2,
                        DeliverCallback deliverCallback, DeliverCallback deliverCallback2) throws IOException {
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
        String[] consumerTags = new String[2];
        consumerTags[0] =
                channels[0].basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {});

        if(queueName2 != null && !queueName2.isEmpty()) {
            consumerTags[1] =
                    channels[1].basicConsume(queueName2, autoAck, deliverCallback2, consumerTag -> { });
        }

        return consumerTags;
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
