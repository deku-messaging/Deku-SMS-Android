package com.example.swob_deku.Services;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.List;

public class RMQConnection {

    private boolean durable = true;
    private boolean exclusive = false;
    private boolean autoDelete = false;
    private boolean autoAck = true;

    private String queueName, bindingKey;

    private Connection connection;

    private Channel channel;

    private DeliverCallback deliverCallback;

    public RMQConnection(Connection connection) throws IOException {
        this.connection = connection;

        this.channel = this.connection.createChannel();
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
        this.bindingKey = bindingKey;
        this.deliverCallback = deliverCallback;

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
        this.channel.basicConsume(this.queueName, autoAck, deliverCallback, consumerTag -> {});
    }
}
