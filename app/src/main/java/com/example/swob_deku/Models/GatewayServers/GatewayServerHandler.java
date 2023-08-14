package com.example.swob_deku.Models.GatewayServers;

import android.content.Context;

import androidx.room.Room;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.Migrations;

public class GatewayServerHandler {
    Datastore databaseConnector;

    public GatewayServerHandler(Context context){
         databaseConnector = Room.databaseBuilder(context, Datastore.class,
                         Datastore.databaseName)
                 .addMigrations(new Migrations.Migration4To5())
                .addMigrations(new Migrations.Migration5To6())
                 .addMigrations(new Migrations.Migration6To7())
                .build();
    }

    public void delete(long id) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                GatewayServer gatewayServer = new GatewayServer();
                gatewayServer.setId(id);
                gatewayServerDAO.delete(gatewayServer);
            }
        });
        thread.start();
        thread.join();
    }

    public void add(GatewayServer gatewayServer) throws InterruptedException {
        gatewayServer.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                gatewayServerDAO.insert(gatewayServer);
            }
        });
        thread.start();
        thread.join();
    }

    public void update(GatewayServer gatewayServer) throws InterruptedException {
        gatewayServer.setDate(System.currentTimeMillis());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                gatewayServerDAO.update(gatewayServer);
            }
        });
        thread.start();
        thread.join();
    }

    public void close() {
        databaseConnector.close();
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
