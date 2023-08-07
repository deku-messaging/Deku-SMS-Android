package com.example.swob_deku.Models.Web;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.swob_deku.MessagesThreadsActivity;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class WebWebsocketsService extends Service {
    final int NOTIFICATION_ID = 1235;

    final String MESSAGE_TYPE_THREADS = "MESSAGE_TYPE_THREADS";
    final String MESSAGE_TYPE_SINGLE = "MESSAGE_TYPE_SINGLE";
    private WebSocketClient webSocketClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            _configureWebsockets();
            _configureSharedPreferenceListeners();
        } catch (URISyntaxException | CertificateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void _configureSharedPreferenceListeners() {

    }

    private void _configureWebsockets() throws URISyntaxException, CertificateException, IOException {
        URI uri = new URI("ws://staging.smswithoutborders.com:16000");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String publicKey = getPublicKeyFromWebSocketProtocol(uri.toString());
                    Log.d(getClass().getName(), "Public key: " + publicKey);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(getClass().getName(), "+ New connection: " +
                        handshakedata.getHttpStatusMessage());
                createForegroundNotification();
            }

            @Override
            public void onMessage(String message) {
                Log.d(getClass().getName(), "+ New Message: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(getClass().getName(), "- Connection closed: "
                        + code + ", " + reason + ", " + remote);
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    private String getPublicKeyFromWebSocketProtocol(String websocketProtocol) throws IOException, InterruptedException {
        websocketProtocol = websocketProtocol.replace("ws://", "https://");
        String[] parts = websocketProtocol.split(":");

        // Transform the websocket protocol to its HTTP equivalent.
        String httpProtocol = "https:" + parts[1];        /*
        if(BuildConfig.DEBUG)
            primaryKeySite = getString(R.string.official_staging_site);
        else
            primaryKeySite = getString(R.string.official_site);

         */
        URL url = new URL(httpProtocol);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        Certificate[] certificate = urlConnection.getServerCertificates();
//                        for(Certificate certificate: certificates[0]) {
//                            PublicKey publicKey = certificate.getPublicKey();
//                            Log.d(getLocalClassName(), "Cert det: " +
//                                    Base64.encodeToString(publicKey.getEncoded(), Base64.NO_PADDING) +
//                                    certificate.getType() );
//                        }
        return Base64.encodeToString(certificate[0].getPublicKey().getEncoded(), Base64.DEFAULT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!webSocketClient.isOpen()) {
            webSocketClient.connect();

            //        Cursor cursor = SMSHandler.fetchAllMessages(getApplicationContext());
            Cursor cursor = SMSHandler.fetchSMSForThreading(getApplicationContext());

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting().serializeNulls();
            Gson gson = gsonBuilder.create();

            SMS.SMSJsonEntity smsJsonEntity = new SMS.SMSJsonEntity();
            smsJsonEntity.setSmsList(cursor);
            smsJsonEntity.type = MESSAGE_TYPE_THREADS;

            Log.d(getClass().getName(), gson.toJson(smsJsonEntity));

            cursor.close();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(webSocketClient.isOpen())
            webSocketClient.close();
    }

    private void createForegroundNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), MessagesThreadsActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        // String description = runningGatewayClientCount + " " + getString(R.string.gateway_client_running_description);
        String description = getString(R.string.deku_websocket_running_description);

        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(), getString(R.string.running_gateway_clients_channel_id))
                        .setContentTitle(getString(R.string.deku_websocket_running))
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
