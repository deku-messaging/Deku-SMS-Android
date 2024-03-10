package com.afkanerd.deku.QueueListener.RMQ;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_UPDATED_BROADCAST_INTENT;
import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.SemaphoreManager;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjectDao;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjects;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerHandler;
import com.afkanerd.deku.Router.Router.RouterHandler;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.Router.RouterItem;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.RecoveryDelayHandler;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.TrafficListener;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class RMQConnectionService extends Service {
    final int NOTIFICATION_ID = 1234;

    private HashMap<Long, Connection> connectionList = new HashMap<>();

    ExecutorService consumerExecutorService = Executors.newFixedThreadPool(10); // Create a pool of 5 worker threads

    private BroadcastReceiver messageStateChangedBroadcast;

    private SharedPreferences sharedPreferences;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    public RMQConnectionService(Context context) {
        attachBaseContext(context);
    }

    // DO NOT DELETE
    public RMQConnectionService() { }

    Datastore databaseConnector;
    @Override
    public void onCreate() {
        super.onCreate();
        if(Datastore.datastore == null || !Datastore.datastore.isOpen())
            Datastore.datastore = Room.databaseBuilder(getApplicationContext(), Datastore.class,
                            Datastore.databaseName)
                    .enableMultiInstanceInvalidation()
                    .build();
        databaseConnector = Datastore.datastore;

        handleBroadcast();

        sharedPreferences = getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);

        registerListeners();
    }

    public int[] getGatewayClientNumbers() {
        int running = 0;
        int reconnecting = 0;

        for(Long keys : connectionList.keySet()) {
            Connection connection = connectionList.get(keys);
            if(connection != null && connection.isOpen())
                ++running;
            else
                ++reconnecting;
        }
        return new int[]{running, reconnecting};
    }

   private void registerListeners() {
       sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
           @Override
           public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
               Log.d(getClass().getName(), "Shared preferences changed: " + key);
               if(connectionList.containsKey(Long.parseLong(key))) {
                   if(connectionList.get(Long.parseLong(key)) != null &&
                           !sharedPreferences.contains(key) ) {
                       consumerExecutorService.execute(new Runnable() {
                           @Override
                           public void run() {
                               try {
                                   stop(Long.parseLong(key));
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }
                           }
                       });
                   } else {
                       int[] states = getGatewayClientNumbers();
                       createForegroundNotification(states[0], states[1]);
                   }
               }
           }
       };
       sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
   }

    private void handleBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SMS_SENT_BROADCAST_INTENT);
//        intentFilter.addAction(SMS_DELIVERED_BROADCAST_INTENT);

        messageStateChangedBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if (intent.getAction() != null && intentFilter.hasAction(intent.getAction())) {
                    if(intent.hasExtra(RMQConnection.MESSAGE_SID) &&
                            intent.hasExtra(RMQConnection.RMQ_DELIVERY_TAG)) {

                        final String sid = intent.getStringExtra(RMQConnection.MESSAGE_SID);
                        final String messageId = intent.getStringExtra(NativeSMSDB.ID);

                        final String consumerTag =
                                intent.getStringExtra(RMQConnection.RMQ_CONSUMER_TAG);
                        final long deliveryTag =
                                intent.getLongExtra(RMQConnection.RMQ_DELIVERY_TAG, -1);
                        assertTrue(deliveryTag != -1);

                        Channel channel = activeConsumingChannels.get(consumerTag);

                        if(intentFilter.hasAction(intent.getAction())) {
                            Log.d(getClass().getName(), "Received an ACK of the message...");
                            if(getResultCode() == Activity.RESULT_OK) {
                                consumerExecutorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.i(getClass().getName(),
                                                    "Confirming message sent");
                                            if(channel == null || !channel.isOpen()) {
                                                return;
                                            }
                                            channel.basicAck(deliveryTag, false);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } else {
                                Log.w(getClass().getName(), "Rejecting message sent");
                                consumerExecutorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if(channel == null || !channel.isOpen()) {
                                                return;
                                            }
                                            channel.basicReject(deliveryTag, true);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        };

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            registerReceiver(messageStateChangedBroadcast, intentFilter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(messageStateChangedBroadcast, intentFilter);
    }

    private DeliverCallback getDeliverCallback(final Channel channel, final int subscriptionId) {
        return (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);

                final String body = jsonObject.getString(RMQConnection.MESSAGE_BODY_KEY);
                final String msisdn = jsonObject.getString(RMQConnection.MESSAGE_MSISDN_KEY);
                final String sid = jsonObject.getString(RMQConnection.MESSAGE_SID);
                long threadId = Telephony.Threads.getOrCreateThreadId(getApplicationContext(), msisdn);

                Bundle bundle = new Bundle();
                bundle.putString(RMQConnection.MESSAGE_SID, sid);
                bundle.putLong(RMQConnection.RMQ_DELIVERY_TAG,
                        delivery.getEnvelope().getDeliveryTag());
                bundle.putString(RMQConnection.RMQ_CONSUMER_TAG, consumerTag);

                SemaphoreManager.acquireSemaphore(subscriptionId);
                long messageId = System.currentTimeMillis();
                Conversation conversation = new Conversation();
                conversation.setMessage_id(String.valueOf(messageId));
                conversation.setText(body);
                conversation.setSubscription_id(subscriptionId);
                conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                conversation.setDate(String.valueOf(System.currentTimeMillis()));
                conversation.setAddress(msisdn);
                conversation.setThread_id(String.valueOf(threadId));
                conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                databaseConnector.threadedConversationsDao().insertThreadAndConversation(conversation);
                Log.d(getClass().getName(), "Sending RMQ SMS: " + subscriptionId + ":"
                        + conversation.getAddress());
                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, bundle);
            } catch (JSONException e) {
                e.printStackTrace();
                consumerExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        if(channel != null && channel.isOpen()) {
                            try {
                                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            } catch(Exception e) {
                e.printStackTrace();
                consumerExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(channel != null && channel.isOpen())
                                channel.basicReject(delivery.getEnvelope().getDeliveryTag(),
                                        true);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } finally {
                try {
                    SemaphoreManager.releaseSemaphore(subscriptionId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void startAllGatewayClientConnections() {
        Log.d(getClass().getName(), "Starting all connections...");
//        connectionList.clear();
        Map<String, ?> storedGatewayClients = sharedPreferences.getAll();
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());

        int[] states = getGatewayClientNumbers();
        createForegroundNotification(states[0], states[1]);

        for (String gatewayClientIds : storedGatewayClients.keySet()) {
            if(!connectionList.containsKey(Long.parseLong(gatewayClientIds)) ||
                    (connectionList.get(Long.parseLong(gatewayClientIds)) != null &&
                            !connectionList.get(Long.parseLong(gatewayClientIds)).isOpen())) {
                try {
                    GatewayClient gatewayClient =
                            gatewayClientHandler.fetch(Long.parseLong(gatewayClientIds));
                    connectGatewayClient(gatewayClient);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAllGatewayClientConnections();
        return START_STICKY;
    }

    public void startConnection(ConnectionFactory factory, GatewayClient gatewayClient) throws IOException, TimeoutException, InterruptedException {
        Log.d(getClass().getName(), "Starting new connection...");

        Connection connection = connectionList.get(gatewayClient.getId());
        if(connection == null || !connection.isOpen()) {
            try {
                connection = factory.newConnection(consumerExecutorService,
                        gatewayClient.getFriendlyConnectionName());
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(5000);
                startConnection(factory, gatewayClient);
            }
        }

        RMQConnection rmqConnection = new RMQConnection(connection);
        connectionList.put(gatewayClient.getId(), connection);

        if(connection != null)
        connection.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                Log.e(getClass().getName(), "Connection shutdown cause: " + cause.toString());
                if(sharedPreferences.getBoolean(String.valueOf(gatewayClient.getId()), false)) {
                    try {
                        connectionList.remove(gatewayClient.getId());
                        int[] states = getGatewayClientNumbers();
                        createForegroundNotification(states[0], states[1]);
                        startConnection(factory, gatewayClient);
                    } catch (IOException | TimeoutException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        GatewayClientProjectDao gatewayClientProjectDao =
                gatewayClientHandler.databaseConnector.gatewayClientProjectDao();

        List<SubscriptionInfo> subscriptionInfoList = SIMHandler
                .getSimCardInformation(getApplicationContext());

        List<GatewayClientProjects> gatewayClientProjectsList =
                gatewayClientProjectDao.fetchGatewayClientIdList(gatewayClient.getId());
        Log.d(getClass().getName(), "Subscription number: " + subscriptionInfoList.size());

        for(int i=0;i<gatewayClientProjectsList.size(); ++i) {
            for(int j=0;j<subscriptionInfoList.size();++j) {
                final Channel channel = rmqConnection.createChannel();
                GatewayClientProjects gatewayClientProjects = gatewayClientProjectsList.get(i);
                String bindingName = j > 0 ? gatewayClientProjects.binding2Name :
                        gatewayClientProjects.binding1Name;
                int subscriptionId = subscriptionInfoList.get(j).getSubscriptionId();

                startChannelConsumption(rmqConnection, channel, subscriptionId,
                        gatewayClientProjects, bindingName);
            }
        }

        int[] states = getGatewayClientNumbers();
        createForegroundNotification(states[0], states[1]);
    }

    public void startChannelConsumption(RMQConnection rmqConnection, final Channel channel,
                                        final int subscriptionId,
                                        final GatewayClientProjects gatewayClientProjects,
                                        final String bindingName) throws IOException {
        channel.basicRecover(true);
        final DeliverCallback deliverCallback = getDeliverCallback(channel, subscriptionId);
        consumerExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String queueName = rmqConnection.createQueue(gatewayClientProjects.name, bindingName,
                            channel, null);
                    long messagesCount = channel.messageCount(queueName);

                    Log.d(getClass().getName(), "Created Queue: " + queueName
                            + " (" + messagesCount + ")");

                    String consumerTag = channel.basicConsume(queueName, false, deliverCallback,
                            new ConsumerShutdownSignalCallback() {
                                @Override
                                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                                    Log.e(getClass().getName(), "Consumer error: " + sig.getMessage());
                                    if(rmqConnection.connection != null && rmqConnection.connection.isOpen()) {
                                        try {
                                            activeConsumingChannels.remove(consumerTag);
                                            Channel channel = rmqConnection.createChannel();
                                            startChannelConsumption(rmqConnection, channel, subscriptionId,
                                                    gatewayClientProjects, bindingName);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                    activeConsumingChannels.put(consumerTag, channel);
                    Log.i(getClass().getName(), "Adding tag: " + consumerTag);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    Map<String, Channel> activeConsumingChannels = new HashMap<>();

    boolean disconnected = false;
    public void connectGatewayClient(GatewayClient gatewayClient) throws InterruptedException {
        Log.d(getClass().getName(), "Starting new service connection...");

        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername(gatewayClient.getUsername());
        factory.setPassword(gatewayClient.getPassword());
        factory.setVirtualHost(gatewayClient.getVirtualHost());
        factory.setHost(gatewayClient.getHostUrl());
        factory.setPort(gatewayClient.getPort());
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000);
        factory.setExceptionHandler(new DefaultExceptionHandler());

        factory.setRecoveryDelayHandler(new RecoveryDelayHandler() {
            @Override
            public long getDelay(int recoveryAttempts) {
                Log.w(getClass().getName(), "Factory recovering...: " + recoveryAttempts);
                int[] states = getGatewayClientNumbers();
                createForegroundNotification(states[0], states[1]);
                disconnected = true;
                return 10000;
            }
        });
        factory.setTrafficListener(new TrafficListener() {
            @Override
            public void write(Command outboundCommand) {
            }

            @Override
            public void read(Command inboundCommand) {
                if(disconnected) {
                    Objects.requireNonNull(connectionList.get(gatewayClient.getId())).abort();
                    connectionList.remove(gatewayClient.getId());
                    startAllGatewayClientConnections();
                    disconnected = false;
                }
            }
        });

        consumerExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                /**
                 * Avoid risk of :ForegroundServiceDidNotStartInTimeException
                 * - Put RMQ connection in list before connecting which could take a while
                 */

                try {
                    startConnection(factory, gatewayClient);
                } catch (IOException | TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stop(long gatewayClientId) throws IOException {
        if(connectionList.containsKey(gatewayClientId)) {
            Connection connection = connectionList.get(gatewayClientId);
            if(connection != null)
                connection.close();

            connectionList.remove(gatewayClientId);
            if(connectionList.isEmpty()) {
                stopForeground(true);
                stopSelf();
            }
            else {
                int[] states = getGatewayClientNumbers();
                createForegroundNotification(states[0], states[1]);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getName(), "Ending connection...");
        if(messageStateChangedBroadcast != null)
            unregisterReceiver(messageStateChangedBroadcast);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void createForegroundNotification(int runningGatewayClientCount, int reconnecting) {
        Intent notificationIntent = new Intent(getApplicationContext(), GatewayClientListingActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        String description = runningGatewayClientCount + " " +
                getString(R.string.gateway_client_running_description);

        if(reconnecting > 0)
            description += "\n" + reconnecting + " " +
                    getString(R.string.gateway_client_reconnecting_description);


        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(),
                        getString(R.string.running_gateway_clients_channel_id))
                        .setContentTitle(getApplicationContext()
                                .getString(R.string.gateway_client_running_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setSilent(true)
                        .setOngoing(true)
                        .setContentText(description)
                        .setContentIntent(pendingIntent)
                        .build();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        else
            startForeground(NOTIFICATION_ID, notification);
    }
}
