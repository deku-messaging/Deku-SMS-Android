package com.example.swob_deku.Models.GatewayServers;

import android.content.Context;

import androidx.room.Room;

import com.example.swob_deku.Models.Datastore;

public class GatewayServerHandler {
    public static void add(Context context, GatewayServer gatewayServer) throws InterruptedException {
        gatewayServer.setDate(System.currentTimeMillis());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName).build();
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                gatewayServerDAO.insert(gatewayServer);
            }
        });
        thread.start();
        thread.join();
    }

//    public static List<GatewayServer> fetchAll(Context context) throws InterruptedException {
//        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
//                Datastore.databaseName).build();
//
//        final List<GatewayServer>[] encryptedContentList = new List[]{new ArrayList<>()};
//
//        Thread fetchEncryptedMessagesThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
//                encryptedContentList[0] = gatewayServerDAO.getAll();
//            }
//        });
//
//        fetchEncryptedMessagesThread.start();
//        fetchEncryptedMessagesThread.join();
//
//        return encryptedContentList[0];
//    }
}
