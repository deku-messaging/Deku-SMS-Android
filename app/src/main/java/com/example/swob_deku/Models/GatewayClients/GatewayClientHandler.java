package com.example.swob_deku.Models.GatewayClients;

import android.content.Context;

import androidx.room.Room;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.Migrations;

public class GatewayClientHandler {

    public static void add(Context context, GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                        .addMigrations(new Migrations.Migration4To5())
                        .build();
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.insert(gatewayClient);
            }
        });
        thread.start();
        thread.join();
    }
}
