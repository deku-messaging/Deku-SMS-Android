package java.com.afkanerd.deku.QueueListener;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.afkanerd.deku.DefaultSMS.Models.Database.SemaphoreManager;
import com.afkanerd.deku.QueueListener.RMQ.RMQConnection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class RMQConnectionTest {

    Context context;

    Properties properties = new Properties();
    ExecutorService consumerExecutorService = Executors.newFixedThreadPool(1); // Create a pool of 5 worker threads

    public RMQConnectionTest() throws IOException {
//        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        InputStream inputStream = this.context.getResources()
//                .openRawResource(R.raw.app);
//        properties.load(inputStream);
    }

    @Test
    public void connectionTest() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(properties.getProperty("username"));
        factory.setPassword(properties.getProperty("password"));
        factory.setVirtualHost(properties.getProperty("virtualhost"));
        factory.setHost(properties.getProperty("host"));
        factory.setPort(Integer.parseInt(properties.getProperty("port")));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000);
        factory.setExceptionHandler(new DefaultExceptionHandler());

        Connection connection = factory.newConnection(consumerExecutorService,
                "android-studio-test-case");

        RMQConnection rmqConnection = new RMQConnection(connection);
        final Channel channel = rmqConnection.createChannel();
        channel.basicRecover(true);

        String defaultExchange = properties.getProperty("exchange");
        String defaultQueueName = "android_studio_testing_queue";
        String defaultQueueName1 = "android_studio_testing_queue1";
        String defaultBindingKey = "#.62401";
        String defaultBindingKey1 = "*.routing.62401";
        String defaultRoutingKey = "testing.routing.62401";

        rmqConnection.createQueue(defaultExchange, defaultBindingKey, channel, defaultQueueName);
        rmqConnection.createQueue(defaultExchange, defaultBindingKey1, channel, defaultQueueName1);
        channel.queuePurge(defaultQueueName);
        channel.queuePurge(defaultQueueName1);

        long messageCount = channel.messageCount(defaultQueueName);
        assertEquals(0, messageCount);

        messageCount = channel.messageCount(defaultQueueName1);
        assertEquals(0, messageCount);

        String basicMessage = "hello world 0";
        channel.basicPublish(defaultExchange, defaultRoutingKey, null,
                basicMessage.getBytes(StandardCharsets.UTF_8));

        Set<String> consumerTags = new HashSet<>();
        final boolean[] shutdownDown = {false};
        ConsumerShutdownSignalCallback consumerShutdownSignalCallback = new ConsumerShutdownSignalCallback() {
            @Override
            public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                shutdownDown[0] = true;
                consumerTags.remove(consumerTag);
                rmqConnection.removeChannel(channel);
            }
        };

        final boolean[] delivered = {false};
        DeliverCallback deliverCallback = new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery message) throws IOException {
                delivered[0] = true;
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            }
        };

        DeliverCallback deliverCallback1 = new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery message) throws IOException {

            }
        };

        String consumerTag = channel.basicConsume(defaultQueueName, false, deliverCallback,
                consumerShutdownSignalCallback);
        consumerTags.add(consumerTag);

        /**
         * This causes an error which forces the channel to close.
         * This behaviour can then be observed.
         */
        try {
            String nonExistentExchangeName = "nonExistentExchangeName";
            String nonExistentBindingName = "nonExistentBindingName";
            rmqConnection.createQueue(nonExistentExchangeName,
                    nonExistentBindingName, channel, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assertTrue(connection.isOpen());
            assertFalse(channel.isOpen());
        }

        assertTrue(delivered[0]);
        assertTrue(shutdownDown[0]);
        assertFalse(consumerTags.contains(consumerTag));

        assertEquals(0, rmqConnection.channelList.size());

        Channel channel1 = rmqConnection.createChannel();
        messageCount = channel1.messageCount(defaultQueueName);
        assertEquals(0, messageCount);

        messageCount = channel1.messageCount(defaultQueueName1);
        assertEquals(1, messageCount);

       channel1.basicConsume(defaultQueueName1, false, deliverCallback1,
               consumerShutdownSignalCallback);

        messageCount = channel1.messageCount(defaultQueueName1);
        assertEquals(0, messageCount);

        try {
            String nonExistentExchangeName = "nonExistentExchangeName";
            String nonExistentBindingName = "nonExistentBindingName";
            rmqConnection.createQueue(nonExistentExchangeName,
                    nonExistentBindingName, channel1, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assertTrue(connection.isOpen());
            assertFalse(channel1.isOpen());
        }

        connection.abort();
        assertFalse(connection.isOpen());
        assertFalse(channel1.isOpen());
    }

    @Test
    public void semaphoreTest() throws InterruptedException {
        final long[] startTime = {0};
        final long[] endTime = {0};

        final long[] startTime1 = {0};
        final long[] endTime1 = {0};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SemaphoreManager.acquireSemaphore();
                    Log.d(getClass().getName(), "Thread 1 acquired!");
                    startTime[0] = System.currentTimeMillis();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        SemaphoreManager.releaseSemaphore();
                        Log.d(getClass().getName(), "Thread 1 released!: " +
                                System.currentTimeMillis());
                        endTime[0] = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(getClass().getName(), "Thread 2 requested!: " +
                            System.currentTimeMillis());
                    SemaphoreManager.acquireSemaphore();
                    Log.d(getClass().getName(), "Thread 2 acquired!: " +
                            System.currentTimeMillis());
                    startTime1[0] = System.currentTimeMillis();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        SemaphoreManager.releaseSemaphore();
                        Log.d(getClass().getName(), "Thread 2 released!: " +
                                System.currentTimeMillis());
                        endTime1[0] = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
        thread1.start();
        thread1.join();
        thread.join();

        assertTrue(endTime[0] <= startTime1[0]);
    }
}
