package com.afkanerd.deku.QueueListener.RMQ;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_UPDATED_BROADCAST_INTENT;
import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

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
import android.telephony.SubscriptionInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.Router.Router.RouterHandler;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.Router.RouterItem;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RMQConnectionService extends Service {
    final int NOTIFICATION_ID = 1234;
    final long DELAY_TIMEOUT = 10000;

    public final static String RMQ_SUCCESS_BROADCAST_INTENT = "RMQ_SUCCESS_BROADCAST_INTENT";
    public final static String RMQ_STOP_BROADCAST_INTENT = "RMQ_STOP_BROADCAST_INTENT";

    public final static String SMS_TYPE_STATUS = "SMS_TYPE_STATUS";
    public final static String SMS_STATUS_SENT = "SENT";
    public final static String SMS_STATUS_DELIVERED = "DELIVERED";
    public final static String SMS_STATUS_FAILED = "FAILED";

    private HashMap<Long, RMQMonitor> connectionList = new HashMap<>();

    ExecutorService consumerExecutorService = Executors.newFixedThreadPool(4); // Create a pool of 5 worker threads

    private BroadcastReceiver messageStateChangedBroadcast;

    private HashMap<String, Map<Long, Channel>> channelList = new HashMap<>();

    private SharedPreferences sharedPreferences;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    Conversation conversation;
    ConversationDao conversationDao;

    public RMQConnectionService(Context context) {
        attachBaseContext(context);
    }

    public RMQConnectionService(){}


    @Override
    public void onCreate() {
        super.onCreate();

        handleBroadcast();

        sharedPreferences = getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);

        registerListeners();

        conversation = new Conversation();
        conversationDao = conversation.getDaoInstance(getApplicationContext());
    }

    public int[] getGatewayClientNumbers() {
        Map<String, ?> keys = sharedPreferences.getAll();
        int running = 0;
        int reconnecting = 0;

        for(String _key : keys.keySet()) {
            if (sharedPreferences.getBoolean(_key, false))
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
                   } else if(connectionList.get(Long.parseLong(key)) != null &&
                           sharedPreferences.contains(key) ){
                       int[] states = getGatewayClientNumbers();
                       createForegroundNotification(states[0], states[1]);
                   }
               }
               else {
                   consumerExecutorService.execute(new Runnable() {
                       @Override
                       public void run() {
                           GatewayClientHandler gatewayClientHandler =
                                   new GatewayClientHandler(getApplicationContext());
                           try {
                               GatewayClient gatewayClient = gatewayClientHandler.fetch(Integer.parseInt(key));
                               connectGatewayClient(gatewayClient);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }
                       }
                   });
               }
           }
       };
       sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
   }

    private void handleBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT);
        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT);

        messageStateChangedBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                // TODO: in case this intent comes back but the internet connection broke to send back acknowledgement
                // TODO: should store pending confirmations in a place
                Log.d(getClass().getName(), "Got request for RMQ broadcast!");

                if (intent.getAction() != null &&
                        intentFilter.hasAction(intent.getAction())) {
                    RouterItem smsStatusReport = new RouterItem();

                    if(intent.hasExtra(RMQConnection.MESSAGE_SID)) {
                        Log.d(getClass().getName(), "RMQ Sid found!");
                        String messageSid = intent.getStringExtra(RMQConnection.MESSAGE_SID);
                        if (intent.getAction().equals(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT)) {
                            Map<Long, Channel> deliveryChannel = channelList.get(messageSid);
                            final Long deliveryTag = deliveryChannel.keySet().iterator().next();
                            Channel channel = deliveryChannel.get(deliveryTag);

                            smsStatusReport.sid = messageSid;
                            if(getResultCode() == Activity.RESULT_OK) {
                                if (channel != null && channel.isOpen()) {
                                    consumerExecutorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                channel.basicAck(deliveryTag, false);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                                smsStatusReport.reportedStatus = SMS_STATUS_SENT;
                            } else {
                                if (channel != null && channel.isOpen()) {
                                    consumerExecutorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                channel.basicReject(deliveryTag, true);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                    smsStatusReport.reportedStatus = SMS_STATUS_FAILED;
                                }
                            }

                        }
                        else if (intent.getAction().equals(IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT)) {
                            smsStatusReport.sid = messageSid;
                            smsStatusReport.reportedStatus = SMS_STATUS_DELIVERED;
                        }

                        consumerExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    RouterHandler.route(context, smsStatusReport);
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else Log.d(getClass().getName(), "Sid not found!");
                }
            }
        };

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            registerReceiver(messageStateChangedBroadcast, intentFilter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(messageStateChangedBroadcast, intentFilter);
    }

    private DeliverCallback getDeliverCallback(Channel channel, final int subscriptionId) {
        return (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                JSONObject jsonObject = new JSONObject(message);

                String body = jsonObject.getString(RMQConnection.MESSAGE_BODY_KEY);

                String msisdn = jsonObject.getString(RMQConnection.MESSAGE_MSISDN_KEY);
                String globalMessageKey = jsonObject.getString(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY);
                String sid = jsonObject.getString(RMQConnection.MESSAGE_SID);

                Map<Long, Channel> deliveryChannelMap = new HashMap<>();
                deliveryChannelMap.put(delivery.getEnvelope().getDeliveryTag(), channel);
                channelList.put(sid, deliveryChannelMap);

                Bundle bundle = new Bundle();
                bundle.putString(RMQConnection.MESSAGE_SID, sid);
                String messageId = String.valueOf(System.currentTimeMillis());

                long threadId = Telephony.Threads.getOrCreateThreadId(getApplicationContext(), msisdn);
                Conversation conversation = new Conversation();
                conversation.setMessage_id(messageId);
                conversation.setText(body);
                conversation.setSubscription_id(subscriptionId);
                conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                conversation.setDate(String.valueOf(System.currentTimeMillis()));
                conversation.setAddress(msisdn);
                conversation.setThread_id(String.valueOf(threadId));
                conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                long id = conversationDao.insert(conversation);
                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, bundle);
//                conversation.setId(id);
//                conversationDao.update(conversation);
            } catch (JSONException e) {
                e.printStackTrace();
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            } catch(Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Map<String, ?> storedGatewayClients = sharedPreferences.getAll();
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());

        for (String gatewayClientIds : storedGatewayClients.keySet()) {
            if(!connectionList.containsKey(Long.parseLong(gatewayClientIds))) {
                try {
                    GatewayClient gatewayClient = gatewayClientHandler.fetch(Long.parseLong(gatewayClientIds));
                    connectGatewayClient(gatewayClient);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return START_STICKY;
    }

    public void connectGatewayClient(GatewayClient gatewayClient) throws InterruptedException {
        Log.d(getClass().getName(), "Starting new service connection...");
        int[] states = getGatewayClientNumbers();

        ConnectionFactory factory = new ConnectionFactory();

        factory.setRecoveryDelayHandler(new RecoveryDelayHandler() {
            @Override
            public long getDelay(int recoveryAttempts) {
                connectionList.get(gatewayClient.getId()).setConnected(DELAY_TIMEOUT);
                return DELAY_TIMEOUT;
            }
        });

        factory.setUsername(gatewayClient.getUsername());
        factory.setPassword(gatewayClient.getPassword());
        factory.setVirtualHost(gatewayClient.getVirtualHost());
        factory.setHost(gatewayClient.getHostUrl());
        factory.setPort(gatewayClient.getPort());
        factory.setConnectionTimeout(15000);
        factory.setExceptionHandler(new DefaultExceptionHandler());

        consumerExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    /**
                     * Avoid risk of :ForegroundServiceDidNotStartInTimeException
                     * - Put RMQ connection in list before connecting which could take a while
                     */

                    RMQConnection rmqConnection = new RMQConnection();

                    RMQMonitor rmqMonitor = new RMQMonitor(getApplicationContext(),
                            gatewayClient.getId(),
                            rmqConnection);
                    connectionList.put(gatewayClient.getId(), rmqMonitor);

                    rmqMonitor.setConnected(DELAY_TIMEOUT);
                    Log.d(getClass().getName(), "Attempting to make connection...");

                    Connection connection = factory.newConnection(consumerExecutorService,
                            gatewayClient.getFriendlyConnectionName());
                    Log.d(getClass().getName(), "Connection made..");

                    rmqMonitor.setConnected(0L);
                    connection.addShutdownListener(new ShutdownListener() {
                        @Override
                        public void shutdownCompleted(ShutdownSignalException cause) {
                            Log.d(getClass().getName(), "Connection shutdown cause: " + cause.toString());
                        }
                    });

                    rmqMonitor.getRmqConnection().setConnection(connection);

                    List<SubscriptionInfo> subscriptionInfoList = SIMHandler
                            .getSimCardInformation(getApplicationContext());

                    if(gatewayClient.getProjectName() != null && !gatewayClient.getProjectName().isEmpty()) {
                        SubscriptionInfo subscriptionInfo = subscriptionInfoList.get(0);
                        DeliverCallback deliverCallback1 = getDeliverCallback(rmqConnection.getChannel1(),
                                subscriptionInfo.getSubscriptionId());
                        DeliverCallback deliverCallback2 = null;

                        boolean dualQueue = subscriptionInfoList.size() > 1 &&
                                gatewayClient.getProjectBinding2() != null &&
                                !gatewayClient.getProjectBinding2().isEmpty();
                        if(dualQueue) {
                            subscriptionInfo = subscriptionInfoList.get(1);
                            deliverCallback2 = getDeliverCallback(rmqConnection.getChannel2(),
                                    subscriptionInfo.getSubscriptionId());
                        }

                        rmqConnection.createQueue(gatewayClient.getProjectName(),
                                gatewayClient.getProjectBinding(), gatewayClient.getProjectBinding2(),
                                deliverCallback1, deliverCallback2);
                        rmqConnection.consume();
                    }
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                    // TODO: send a notification indicating this, with options to retry the connection
                    int[] states = getGatewayClientNumbers();
                    createForegroundNotification(states[0], states[1]);
                }
            }
        });
    }

    private void stop(long gatewayClientId) {
        try {
            if(connectionList.containsKey(gatewayClientId)) {
                connectionList.remove(gatewayClientId)
                        .getRmqConnection().close();
                if(connectionList.isEmpty()) {
                    stopForeground(true);
                    stopSelf();
                }
                else {
                    int[] states = getGatewayClientNumbers();
                    createForegroundNotification(states[0], states[1]);
                }
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

    public void createForegroundNotification(int runningGatewayClientCount, int reconnecting) {
//        Intent notificationIntent = new Intent(context, GatewayClientListingActivity.class);
//        if(context == null) {
//            context = getApplicationContext();
//            attachBaseContext(context);
//        }
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
