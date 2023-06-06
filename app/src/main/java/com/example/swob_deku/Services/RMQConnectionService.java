package com.example.swob_deku.Services;

import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.example.swob_deku.Models.GatewayClients.GatewayClientRecyclerAdapter;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
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
    public final static String RMQ_SUCCESS_BROADCAST_INTENT = "RMQ_SUCCESS_BROADCAST_INTENT";
    public final static String RMQ_STOP_BROADCAST_INTENT = "RMQ_STOP_BROADCAST_INTENT";

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

        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d(getClass().getName(), "Shared preferences changed: " + key);

                if(connectionList.containsKey(Integer.parseInt(key)) &&
                        connectionList.get(Integer.parseInt(key)) != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            int adapterPosition = intent.getIntExtra(GatewayClientRecyclerAdapter.ADAPTER_POSITION, -1);
//                            stopService(gatewayClientId, adapterPosition);
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

        final Integer gatewayClientId = intent.getIntExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1);
        if(!connectionList.containsKey(gatewayClientId)) {
            String username = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_USERNAME);
            String password = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_PASSWORD);
            String virtualHost = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_VIRTUAL_HOST);
            String host = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_HOST);
            String friendlyName = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_FRIENDLY_NAME);
            int port = intent.getIntExtra(GatewayClientListingActivity.GATEWAY_CLIENT_PORT,
                    Integer.parseInt(getResources().getString(R.string.settings_gateway_client_default_port)));

            Log.d(getClass().getName(), "Starting new service connection...");
            ConnectionFactory factory = new ConnectionFactory();

            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
            factory.setHost(host);
            factory.setPort(port);
            factory.setConnectionTimeout(15000);
            factory.setExceptionHandler(new DefaultExceptionHandler());

            // Perform your background task
            Log.d(this.getClass().getName(), "Starting rmq connection: " + username + ":" + password + "@" + host);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Connection connection = null;
                    int adapterPosition = intent.getIntExtra(GatewayClientRecyclerAdapter.ADAPTER_POSITION, -1);
                    try {
                        connection = factory.newConnection(consumerExecutorService, friendlyName);

                        RMQConnection rmqConnection = new RMQConnection(connection);

                        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
                        GatewayClient gatewayClient = gatewayClientHandler.fetch(gatewayClientId);
                        gatewayClientHandler.close();

                        if(gatewayClient.getProjectName() != null && !gatewayClient.getProjectName().isEmpty()) {
                            rmqConnection.createQueue(gatewayClient.getProjectName(),
                                    gatewayClient.getProjectBinding(), getDeliverCallback(rmqConnection));
                            rmqConnection.consume();
                        }
                        connectionList.put(gatewayClientId, rmqConnection);
                        broadcastIntent(getApplicationContext(), RMQ_SUCCESS_BROADCAST_INTENT,
                                gatewayClientId, adapterPosition);

                    } catch (IOException | TimeoutException | InterruptedException e) {
                        e.printStackTrace();
                        stopService(gatewayClientId, adapterPosition);
                    }
                }
            }).start();
        }
        else if(intent.hasExtra(GatewayClientListingActivity.GATEWAY_CLIENT_STOP_LISTENERS)) {
            if(connectionList.containsKey(gatewayClientId) && connectionList.get(gatewayClientId) != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int adapterPosition = intent.getIntExtra(GatewayClientRecyclerAdapter.ADAPTER_POSITION, -1);
                        stopService(gatewayClientId, adapterPosition);
                    }
                }).start();
            }
        }

        // return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    private void stopService(int gatewayClientId, int adapterPosition) {
        try {
            if(connectionList.containsKey(gatewayClientId))
                connectionList.remove(gatewayClientId).close();
            broadcastIntent(getApplicationContext(), RMQ_STOP_BROADCAST_INTENT, gatewayClientId, adapterPosition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastIntent(Context context, String broadcastIntent, int id, int adapterPosition) {
        Intent intent = new Intent(broadcastIntent);
        intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, id);
        intent.putExtra(GatewayClientRecyclerAdapter.ADAPTER_POSITION, adapterPosition);
        context.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
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
}
