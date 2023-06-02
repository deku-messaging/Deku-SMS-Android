package com.example.swob_deku.Services;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.List;

public class RMQConnection {

    boolean durable = true;
    boolean exclusive = false;
    boolean autoDelete = false;
    boolean autoAck = true;

    String queueName;

    Connection connection;

    Channel channel;

    DeliverCallback deliverCallback;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void createQueue(String queueName, DeliverCallback deliverCallback) throws IOException {
        this.queueName = queueName;
        this.deliverCallback = deliverCallback;

        this.channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        this.channel.queueBind(queueName, exchangeName, routingKey);
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
