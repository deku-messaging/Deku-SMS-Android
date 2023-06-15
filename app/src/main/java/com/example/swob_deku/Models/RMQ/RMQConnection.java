package com.example.swob_deku.Models.RMQ;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.List;

public class RMQConnection {

    public static final String MESSAGE_BODY_KEY = "text";
    public static final String MESSAGE_MSISDN_KEY = "to";
    public static final String MESSAGE_GLOBAL_MESSAGE_ID_KEY = "id";

    private String queueName;

    private Connection connection;

    private Channel channel;

    private boolean reconnecting = false;

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

    private DeliverCallback deliverCallback;

    public RMQConnection(Connection connection) throws IOException {
        this.setConnection(connection);
    }

    public RMQConnection(){
    }

    public void setConnection(Connection connection) throws IOException {
        this.connection = connection;

        this.channel = this.connection.createChannel();
        int prefetchCount = 1;
        this.channel.basicQos(prefetchCount);
    }

    public void close() throws IOException {
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     *
     * @param exchangeName
     * @param deliverCallback
     * @throws IOException
     */
    public void createQueue(String exchangeName, String bindingKey, DeliverCallback deliverCallback) throws IOException {
        this.queueName = bindingKey.replaceAll("\\.", "_");
        this.deliverCallback = deliverCallback;

        boolean autoDelete = false;
        boolean exclusive = false;
        boolean durable = true;
        this.channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        this.channel.queueBind(queueName, exchangeName, bindingKey);
    }

    public void consume() throws IOException {
        /**
         * - Binding information dumb:
         * 1. .usd. = <anything>.usd.</anything>
         * 2. *.usd = <single anything>.usd
         * 3. #.usd = <many anything>.usd
         * 4. Can all be used in combination with each
         * 5. We can translate this into managing multiple service providers
         */
        boolean autoAck = false;
        this.channel.basicConsume(this.queueName, autoAck, deliverCallback, consumerTag -> {});
    }
}
