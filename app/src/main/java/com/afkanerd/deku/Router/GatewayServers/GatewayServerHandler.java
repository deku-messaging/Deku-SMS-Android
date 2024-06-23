package com.afkanerd.deku.Router.GatewayServers;

import com.afkanerd.deku.Datastore;

import java.util.ArrayList;
import java.util.List;

public class GatewayServerHandler {
    Datastore databaseConnector;

    public synchronized List<GatewayServer> getAll() throws InterruptedException {
        final List<GatewayServer>[] gatewayServerList = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                gatewayServerList[0] = gatewayServerDAO.getAllList();
            }
        });
        thread.start();
        thread.join();

        return gatewayServerList[0];
    }

    public GatewayServer get(long id) throws InterruptedException {
        final GatewayServer[] gatewayServer = {new GatewayServer()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                gatewayServer[0] = gatewayServerDAO.get(String.valueOf(id));
            }
        });
        thread.start();
        thread.join();

        return gatewayServer[0];
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
}
