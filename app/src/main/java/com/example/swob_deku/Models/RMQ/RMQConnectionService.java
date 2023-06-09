package com.example.swob_deku.Models.RMQ;

import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.example.swob_deku.Models.RMQ.RMQConnection;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.RecoveryDelayHandler;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RMQConnectionService extends Service {
    final int NOTIFICATION_ID = 1234;
    public final static String RMQ_SUCCESS_BROADCAST_INTENT = "RMQ_SUCCESS_BROADCAST_INTENT";
    public final static String RMQ_STOP_BROADCAST_INTENT = "RMQ_STOP_BROADCAST_INTENT";

    private int runningGatewayClientCount = 0;

    private HashMap<Integer, RMQConnection> connectionList = new HashMap<>();

    private ExecutorService consumerExecutorService;

    private BroadcastReceiver messageStateChangedBroadcast;

    private HashMap<Long, Map<Long, Channel>> channelList = new HashMap<>();

    private SharedPreferences sharedPreferences;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    public void onCreate() {
        super.onCreate();

        consumerExecutorService = Executors.newFixedThreadPool(5); // Create a pool of 5 worker threads
        handleBroadcast();

        sharedPreferences = getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);

        registerListeners();

    }

   private void registerListeners() {
       sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
           @Override
           public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
               Log.d(getClass().getName(), "Shared preferences changed: " + key);
               if(connectionList.containsKey(Integer.parseInt(key)) &&
                       connectionList.get(Integer.parseInt(key)) != null &&
                       !sharedPreferences.contains(key) ) {
                   new Thread(new Runnable() {
                       @Override
                       public void run() {
                           try {
                               stop(Integer.parseInt(key));
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                       }
                   }).start();
               } else {
                   new Thread(new Runnable() {
                       @Override
                       public void run() {
                           GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
                           try {
                               GatewayClient gatewayClient = gatewayClientHandler.fetch(Integer.parseInt(key));
                               connectGatewayClient(gatewayClient);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }
                       }
                   }).start();
               }
           }
       };
       sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
   }

    private void handleBroadcast() {
        messageStateChangedBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                // TODO: in case this intent comes back but the internet connection broke to send back acknowledgement
                // TODO: should store pending confirmations in a place

                Log.d(getClass().getName(), "Contains required: " + intent.hasExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY));

                if(intent.hasExtra(IncomingTextSMSReplyActionBroadcastReceiver.BROADCAST_STATE) &&
                        intent.getStringExtra(IncomingTextSMSReplyActionBroadcastReceiver.BROADCAST_STATE)
                                .equals(IncomingTextSMSReplyActionBroadcastReceiver.SENT_BROADCAST_INTENT) &&
                        intent.hasExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY)) {
                    Log.d(getClass().getName(), "Service received a broadcast and should acknowledge to rmq from here");

                    long globalMessageId = intent.getLongExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY, -1);
                    if(globalMessageId != -1) {
                        Map<Long, Channel> deliveryChannel = channelList.get(globalMessageId);
                        Long deliveryTag = deliveryChannel.keySet().iterator().next();
                        Channel channel = deliveryChannel.get(deliveryTag);
                        if(channel != null && channel.isOpen()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        channel.basicAck(deliveryTag, false);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                }
            }
        };

        registerReceiver(messageStateChangedBroadcast,
                new IntentFilter(SMSHandler.MESSAGE_STATE_CHANGED_BROADCAST_INTENT));
    }

    private DeliverCallback getDeliverCallback(RMQConnection rmqConnection) {
        return new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery delivery) throws IOException {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    JSONObject jsonObject = new JSONObject(message);

                    String body = jsonObject.getString(RMQConnection.MESSAGE_BODY_KEY);
                    Log.d(getClass().getName(), "New request body: " + body);

                    String msisdn = jsonObject.getString(RMQConnection.MESSAGE_MSISDN_KEY);
                    String globalMessageKey = jsonObject.getString(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY);

                    long messageId = Helpers.generateRandomNumber();

                    PendingIntent[] pendingIntents = IncomingTextSMSBroadcastReceiver
                            .getPendingIntentsForServerRequest(getApplicationContext(), messageId,
                                    Long.parseLong(globalMessageKey));

                    // TODO: fix subscriptionId to actually be the value
                    SMSHandler.sendTextSMS(getApplicationContext(), msisdn, body,
                            pendingIntents[0], pendingIntents[1], messageId, null);

                    Map<Long, Channel> deliveryChannelMap = new HashMap<>();
                    deliveryChannelMap.put(delivery.getEnvelope().getDeliveryTag(), rmqConnection.getChannel());

                    channelList.put(Long.valueOf(globalMessageKey), deliveryChannelMap);
                } catch (JSONException e) {
                    e.printStackTrace();
                    rmqConnection.getChannel().basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getName(), "Request to start service received...");
        Map<String, ?> storedGatewayClients = sharedPreferences.getAll();
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        for (String gatewayClientIds : storedGatewayClients.keySet()) {
            try {
                GatewayClient gatewayClient = gatewayClientHandler.fetch(Integer.parseInt(gatewayClientIds));
                Log.d(getClass().getName(), "* Starting new RMQ connection: " + gatewayClient.getFriendlyConnectionName());
                connectGatewayClient(gatewayClient);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return START_STICKY;
    }

    public void connectGatewayClient(GatewayClient gatewayClient) {
        if(!connectionList.containsKey(gatewayClient.getId())) {
            Log.d(getClass().getName(), "Starting new service connection...");
            ConnectionFactory factory = new ConnectionFactory();
            factory.setRecoveryDelayHandler(new RecoveryDelayHandler() {
                @Override
                public long getDelay(int recoveryAttempts) {
                    // TODO: send notification informing reconnecting is being attempted
                    Log.d(getClass().getName(), "Attempting auto recovery...");
                    return 10000;
                } });

            factory.setUsername(gatewayClient.getUsername());
            factory.setPassword(gatewayClient.getPassword());
            factory.setVirtualHost(gatewayClient.getVirtualHost());
            factory.setHost(gatewayClient.getHostUrl());
            factory.setPort(gatewayClient.getPort());
            factory.setConnectionTimeout(15000);
            factory.setExceptionHandler(new DefaultExceptionHandler());

            // TODO: create a full handler to manage the retry to connection
            // TODO: which matches the Android WorkManager methods
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Connection connection = factory.newConnection(consumerExecutorService,
                                gatewayClient.getFriendlyConnectionName());

                        connection.addShutdownListener(new ShutdownListener() {
                            @Override
                            public void shutdownCompleted(ShutdownSignalException cause) {
                                Log.d(getClass().getName(), "Connection shutdown cause: " + cause.toString());
                            }
                        });

                        RMQConnection rmqConnection = new RMQConnection(connection);

                        if(gatewayClient.getProjectName() != null && !gatewayClient.getProjectName().isEmpty()) {
                            rmqConnection.createQueue(gatewayClient.getProjectName(),
                                    gatewayClient.getProjectBinding(), getDeliverCallback(rmqConnection));
                            rmqConnection.consume();
                        }
                        connectionList.put(gatewayClient.getId(), rmqConnection);
                        sharedPreferences.edit().putLong(String.valueOf(gatewayClient.getId()),
                                System.currentTimeMillis()).apply();

                        createForegroundNotification(++runningGatewayClientCount);

                    } catch (IOException | TimeoutException e) {
                        e.printStackTrace();
//                        if(e.getCause() instanceof ShutdownSignalException) {
//
//                        }
                        // TODO: send a notification indicating this, with options to retry the connection
                        createForegroundNotification(runningGatewayClientCount);
                    }
                }
            }).start();
        }
        // return super.onStartCommand(intent, flags, startId);
    }

    private void stop(int gatewayClientId) {
        try {
            if(connectionList.containsKey(gatewayClientId)) {
                connectionList.remove(gatewayClientId).close();
                if(connectionList.isEmpty()) {
                    stopForeground(true);
                    stopSelf();
                }
                else
                    createForegroundNotification(--runningGatewayClientCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private void createForegroundNotification(int runningGatewayClientCount) {
        Intent notificationIntent = new Intent(getApplicationContext(), GatewayClientListingActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        String description = runningGatewayClientCount + " " + getString(R.string.gateway_client_running_description);
        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(), getString(R.string.running_gateway_clients_channel_id))
                        .setContentTitle(getString(R.string.gateway_client_running_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setSilent(true)
                        .setOngoing(true)
                        .setContentText(description)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(NOTIFICATION_ID, notification);
    }
}
