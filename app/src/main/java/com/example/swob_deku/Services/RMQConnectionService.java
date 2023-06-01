package com.example.swob_deku.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.Models.GatewayClients.GatewayClientRecyclerAdapter;
import com.example.swob_deku.R;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RMQConnectionService extends Service {
    public final static String RMQ_SUCCESS_BROADCAST_INTENT = "RMQ_SUCCESS_BROADCAST_INTENT";
    public final static String RMQ_STOP_BROADCAST_INTENT = "RMQ_STOP_BROADCAST_INTENT";

    HashMap<Integer, Connection> connectionList = new HashMap<>();

    private ExecutorService consumerExecutorService;

    @Override
    public void onCreate() {
        super.onCreate();
        consumerExecutorService = Executors.newFixedThreadPool(5); // Create a pool of 5 worker threads
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
                        connectionList.put(gatewayClientId, connection);
                        broadcastIntent(getApplicationContext(), RMQ_SUCCESS_BROADCAST_INTENT,
                                gatewayClientId, adapterPosition);
                    } catch (IOException | TimeoutException e) {
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
//        consumerExecutorService.shutdown(); // Shutdown the executor when the service is destroyed    }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
