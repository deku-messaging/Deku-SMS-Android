package com.afkanerd.deku.QueueListener.RMQ;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT;
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
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationHandler;
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
import java.util.ArrayList;
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

    private HashMap<Long, Pair<Pair<String, Delivery>, Channel>> channelList = new HashMap<>();

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
            Log.d(getClass().getName(), "Shared_pref checking key: " + _key);
            if (sharedPreferences.getBoolean(_key, false))
                ++running;
            else
                ++reconnecting;
        }

        return new int[]{running, reconnecting};
//        return new int[]{0, 0};
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
               else if(sharedPreferences.contains(key)){
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
        intentFilter.addAction(SMS_SENT_BROADCAST_INTENT);
        intentFilter.addAction(SMS_DELIVERED_BROADCAST_INTENT);

        messageStateChangedBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                // TODO: in case this intent comes back but the internet connection broke to send back acknowledgement
                // TODO: should store pending confirmations in a place

                if (intent.getAction() != null && intentFilter.hasAction(intent.getAction())) {
                    RouterItem smsStatusReport = new RouterItem();

                    if(intent.hasExtra(RMQConnection.MESSAGE_SID)) {
                        consumerExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                String messageSid = intent.getStringExtra(RMQConnection.MESSAGE_SID);
                                final String messageId = intent.getStringExtra(NativeSMSDB.ID);

                                if(messageId == null) {
                                    Log.e(getClass().getName(), "Message ID not found: " + messageSid);
                                    return;
                                }

                                Pair<Pair<String, Delivery>, Channel> consumerDeliveryTagChannel =
                                        channelList.get(Long.parseLong(messageId));

                                if(consumerDeliveryTagChannel == null) {
                                    Log.e(getClass().getName(), "ConsumerDeliveryTagChannel is null");
                                    return;
                                }

                                final String consumerTag = consumerDeliveryTagChannel.first.first;
                                final Delivery deliveryTag = consumerDeliveryTagChannel.first.second;

                                Channel channel = consumerDeliveryTagChannel.second;
                                smsStatusReport.sid = messageSid;

                                if (channel != null && channel.isOpen()) {
                                    Log.i(getClass().getName(), "Received an ACK of the message...");
                                    try {
                                        if(intentFilter.hasAction(intent.getAction())) {
//                                            if(consumerTagChannels == null ||
//                                                    !consumerTagChannels.containsKey(consumerTag)) {
//                                                Log.e(getClass().getName(),
//                                                        "Consumer tag not found - should not reject nor ack: " +
//                                                                consumerTag);
//                                                return;
//                                            }
                                            if(!channelList.containsKey(Long.parseLong(messageId)))
                                                return;
                                            if(getResultCode() == Activity.RESULT_OK) {
//                                                channel.basicAck(deliveryTag, false);
                                                Log.i(getClass().getName(), "Confirming message sent");
                                                channel.basicAck(deliveryTag.getEnvelope().getDeliveryTag(), false);
                                                smsStatusReport.reportedStatus = SMS_STATUS_SENT;
                                            } else {
                                                Log.e(getClass().getName(),
                                                        "Failed to send sms: " + messageId);
                                                channel.basicNack(deliveryTag.getEnvelope().getDeliveryTag(),
                                                        false, true);
                                                smsStatusReport.reportedStatus = SMS_STATUS_FAILED;
                                            }
                                            channelList.remove(Long.parseLong(messageId));
                                        }
//                                        try {
//                                            GatewayServerHandler gatewayServerHandler =
//                                                    new GatewayServerHandler(context);
//                                            RouterHandler.route(context, smsStatusReport, gatewayServerHandler);
//                                        }catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
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
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);

                String body = jsonObject.getString(RMQConnection.MESSAGE_BODY_KEY);

                String msisdn = jsonObject.getString(RMQConnection.MESSAGE_MSISDN_KEY);
                String globalMessageKey = jsonObject.getString(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY);
                String sid = jsonObject.getString(RMQConnection.MESSAGE_SID);

                Bundle bundle = new Bundle();
                bundle.putString(RMQConnection.MESSAGE_SID, sid);

                long messageId = System.currentTimeMillis();
                long threadId = Telephony.Threads.getOrCreateThreadId(getApplicationContext(), msisdn);
                Conversation conversation = new Conversation();
                conversation.setMessage_id(String.valueOf(messageId));
                conversation.setText(body);
                conversation.setSubscription_id(subscriptionId);
                conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                conversation.setDate(String.valueOf(System.currentTimeMillis()));
                conversation.setAddress(msisdn);
                conversation.setThread_id(String.valueOf(threadId));
                conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                conversationDao.insert(conversation);
                Log.d(getClass().getName(), "channel open: " + channel.isOpen());
                Log.d(getClass().getName(), "Sending RMQ SMS: " + conversation.getText() + ":"
                        + conversation.getAddress());
                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, bundle);

                Pair<String, Delivery> consumerTagDelivery = new Pair<>(consumerTag, delivery);
                channelList.put(messageId, new Pair<>(consumerTagDelivery, channel));
            } catch (JSONException e) {
                e.printStackTrace();
                if(channel != null && channel.isOpen())
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

        List<GatewayClient> connectedGatewayClients = new ArrayList<>();
        for (String gatewayClientIds : storedGatewayClients.keySet()) {
            if(!connectionList.containsKey(Long.parseLong(gatewayClientIds))) {
                try {
                    GatewayClient gatewayClient = gatewayClientHandler.fetch(Long.parseLong(gatewayClientIds));
                    if(gatewayClient != null && !connectedGatewayClients.contains(gatewayClient)) {
                        connectGatewayClient(gatewayClient);
                        connectedGatewayClients.add(gatewayClient);
                    } else {
                        sharedPreferences.edit().remove(gatewayClientIds).commit();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return START_STICKY;
    }

    public void startConnection(ConnectionFactory factory, GatewayClient gatewayClient) throws IOException, TimeoutException {
        Log.d(getClass().getName(), "Staring new connection...");
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
//                if(!cause.isInitiatedByApplication())
                if(sharedPreferences.getBoolean(String.valueOf(gatewayClient.getId()), false))
                    try {
                        consumerTagChannels = new HashMap<>();
                        connectionList.remove(gatewayClient.getId());
                        startConnection(factory, gatewayClient);
                    } catch (IOException | TimeoutException e) {
                        e.printStackTrace();
                    }
            }
        });

        List<SubscriptionInfo> subscriptionInfoList = SIMHandler
                .getSimCardInformation(getApplicationContext());

        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        GatewayClientProjectDao gatewayClientProjectDao =
                gatewayClientHandler.databaseConnector.gatewayClientProjectDao();

        List<GatewayClientProjects> gatewayClientProjectsList =
                gatewayClientProjectDao.fetchGatewayClientIdList(gatewayClient.getId());

        SubscriptionInfo subscriptionInfo = subscriptionInfoList.get(0);

        rmqMonitor.getRmqConnection().connection = connection;
        for(GatewayClientProjects gatewayClientProjects : gatewayClientProjectsList) {
//            Channel[] channels = rmqMonitor.getRmqConnection().setConnection(connection);
            Channel[] channels = rmqMonitor.getRmqConnection().getChannels();
            boolean dualQueue = subscriptionInfoList.size() > 1 &&
                    gatewayClientProjects.binding2Name != null &&
                    !gatewayClientProjects.binding2Name.isEmpty();

            DeliverCallback deliverCallback1 =
                    getDeliverCallback(channels[0], subscriptionInfo.getSubscriptionId());
            DeliverCallback deliverCallback2 = null;

            if(dualQueue) {
                subscriptionInfo = subscriptionInfoList.get(1);
                deliverCallback2 = getDeliverCallback(channels[1], subscriptionInfo.getSubscriptionId());
            }

            String[] queues = rmqConnection.createQueue(gatewayClientProjects.name,
                    gatewayClientProjects.binding1Name, gatewayClientProjects.binding2Name, channels);

            String[] consumerTags =
                    rmqConnection.consume(channels, queues[0], queues[1], deliverCallback1,
                            deliverCallback2);
            consumerTagChannels.put(consumerTags[0], channels[0]);
            consumerTagChannels.put(consumerTags[1], channels[1]);

            CustomChannelShutdownListener customChannelShutdownListener =
                    new CustomChannelShutdownListener(consumerTags[0], channels[0]);
            CustomChannelShutdownListener customChannelShutdownListener1 =
                    new CustomChannelShutdownListener(consumerTags[1], channels[1]);

            channels[0].addShutdownListener(customChannelShutdownListener);
            channels[1].addShutdownListener(customChannelShutdownListener1);
        }
    }
    Map<String, Channel> consumerTagChannels = new HashMap<>();

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
        factory.setAutomaticRecoveryEnabled(true);
        factory.setExceptionHandler(new DefaultExceptionHandler());

        consumerExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                /**
                 * Avoid risk of :ForegroundServiceDidNotStartInTimeException
                 * - Put RMQ connection in list before connecting which could take a while
                 */

                try {
                    startConnection(factory, gatewayClient);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
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

    private class CustomChannelShutdownListener implements ShutdownListener {
        public Channel channel;
        public String consumerTag;

        public CustomChannelShutdownListener(String consumerTag, Channel channel) {
            this.channel = channel;
            this.consumerTag = consumerTag;
        }

        @Override
        public void shutdownCompleted(ShutdownSignalException cause) {
            Log.d(getClass().getName(), "Channel shutdown caused: " + cause.getMessage());
//            consumerTagChannels.remove(consumerTag);
//            try {
//                this.channel.basicRecover();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                channel.getConnection().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
