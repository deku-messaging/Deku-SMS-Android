package com.example.swob_deku.Models.Web;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebWebsockets {

    private WebSocketClient webSocketClient;

    public WebWebsockets(URI uri) {
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {

            }

            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };
    }

    public void connect(){
        webSocketClient.connect();
    }
}
