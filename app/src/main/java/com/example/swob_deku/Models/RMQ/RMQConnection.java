package com.example.swob_deku.Models.RMQ;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;

public class RMQConnection {
    final boolean autoDelete = false;
    final boolean exclusive = false;
    final boolean durable = true;
    final boolean autoAck = false;

    public static final String MESSAGE_BODY_KEY = "text";
    public static final String MESSAGE_MSISDN_KEY = "to";
    public static final String MESSAGE_GLOBAL_MESSAGE_ID_KEY = "id";

    private String queueName, queueName2;

    private Connection connection;

    private Channel channel1, channel2;

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
    public void createQueue1(String exchangeName, String bindingKey, DeliverCallback deliverCallback) throws IOException {
        this.queueName = bindingKey.replaceAll("\\.", "_");
        this.deliverCallback = deliverCallback;

        this.channel1.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        this.channel1.queueBind(queueName, exchangeName, bindingKey);
    }

    public void createQueue2(String exchangeName, String bindingKey, DeliverCallback deliverCallback) throws IOException {
        this.queueName2 = bindingKey.replaceAll("\\.", "_");
        this.deliverCallback = deliverCallback;

        this.channel2.queueDeclare(queueName2, durable, exclusive, autoDelete, null);
        this.channel2.queueBind(queueName2, exchangeName, bindingKey);
    }

    public void consume1() throws IOException {
        /**
         * - Binding information dumb:
         * 1. .usd. = <anything>.usd.</anything>
         * 2. *.usd = <single anything>.usd
         * 3. #.usd = <many anything>.usd
         * 4. Can all be used in combination with each
         * 5. We can translate this into managing multiple service providers
         */
        this.channel1.basicConsume(this.queueName, autoAck, deliverCallback, consumerTag -> {});
    }

    public void consume2() throws IOException {
        this.channel2.basicConsume(this.queueName2, autoAck, deliverCallback, consumerTag -> {});
    }
}
