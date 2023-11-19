package com.afkanerd.deku.DefaultSMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import org.json.JSONObject;

import java.util.List;

public class LinkedDevicesQRActivity extends AppCompatActivity {

    private CodeScanner codeScanner;

    public final String WEB_LINKED_INCOMING = "incoming";
    public final String WEB_LINKED_OUTGOING = "outgoing";

    public final String WEB_LINKED_INCOMING_URL = "url";
    public final String WEB_LINKED_INCOMING_TAG = "tag";

    public final String WEB_LINKED_OUTGOING_URL = "url";
    public final String WEB_LINKED_OUTGOING_PORT = "port";
    public final String WEB_LINKED_OUTGOING_USERNAME = "username";
    public final String WEB_LINKED_OUTGOING_PASSWORD = "password";
    public final String WEB_LINKED_OUTGOING_FRIENDLY_NAME = "friendly_name";
    public final String WEB_LINKED_OUTGOING_VIRTUAL_HOST = "virtual_host";
    public final String WEB_LINKED_OUTGOING_PROJECT_NAME = "project_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);


        Toolbar myToolbar = (Toolbar) findViewById(R.id.activity_web_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CodeScannerView scannerView = findViewById(R.id.web_qr_scanner_view);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setAutoFocusEnabled(true);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
//                            String resultValue = result.getText();
//
//                            JSONObject jsonObject = new JSONObject(resultValue);
//
//                            JSONObject incomingObject = jsonObject.getJSONObject(WEB_LINKED_INCOMING);
//                            JSONObject outgoingObject = jsonObject.getJSONObject(WEB_LINKED_OUTGOING);
//
//                            String tag = incomingObject.getString(WEB_LINKED_INCOMING_TAG);
//                            String url = incomingObject.getString(WEB_LINKED_INCOMING_URL);
//
//                            String rmqUrl = outgoingObject.getString(WEB_LINKED_OUTGOING_URL);
//                            int rmqPort = outgoingObject.getInt(WEB_LINKED_OUTGOING_PORT);
//                            String rmqUsername = outgoingObject.getString(WEB_LINKED_OUTGOING_USERNAME);
//                            String rmqPassword = outgoingObject.getString(WEB_LINKED_OUTGOING_PASSWORD);
//                            String rmqFriendlyName = outgoingObject.getString(WEB_LINKED_OUTGOING_FRIENDLY_NAME);
//                            String rmqVirtualHost = outgoingObject.getString(WEB_LINKED_OUTGOING_VIRTUAL_HOST);
//                            String rmqProjectName = outgoingObject.getString(WEB_LINKED_OUTGOING_PROJECT_NAME);
//
//                            GatewayServer gatewayServer = new GatewayServer();
//                            gatewayServer.setTag(tag);
//                            gatewayServer.setURL(url);
//
//                            GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(getApplicationContext());
//                            gatewayServerHandler.add(gatewayServer);
//                            gatewayServerHandler.close();
//
//                            Toast.makeText(getApplicationContext(),
//                                            getString(R.string.linked_devices_new_gateway_server_added),
//                                            Toast.LENGTH_SHORT)
//                                    .show();
//
//                            GatewayClient gatewayClient = new GatewayClient();
//                            gatewayClient.setHostUrl(rmqUrl);
//                            gatewayClient.setPort(rmqPort);
//                            gatewayClient.setUsername(rmqUsername);
//                            gatewayClient.setPassword(rmqPassword);
//                            gatewayClient.setFriendlyConnectionName(rmqFriendlyName);
//                            gatewayClient.setVirtualHost(rmqVirtualHost);
//                            gatewayClient.setProjectName(rmqProjectName);
//
//                            List<String> bindings = GatewayClientHandler.getPublisherDetails(getApplicationContext(),
//                                    rmqProjectName);
//
//                            gatewayClient.setProjectBinding(bindings.get(0));
//                            if(bindings.size() > 1)
//                                gatewayClient.setProjectBinding2(bindings.get(1));
//
//                            GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
//                            long id = gatewayClientHandler.add(gatewayClient);
//                            gatewayClientHandler.close();
//
//
////                            Intent intent = new Intent(getApplicationContext(), GatewayClientCustomizationActivity.class);
////                            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, id);
//                            gatewayClient.setId(id);
//                            GatewayClientHandler.setListening(getApplicationContext(), gatewayClient);
//
//                            Toast.makeText(getApplicationContext(),
//                                            getString(R.string.linked_devices_new_gateway_client_added),
//                                            Toast.LENGTH_SHORT)
//                                    .show();
//                            Intent intent = new Intent(getApplicationContext(), ThreadedConversationsActivity.class);
//                            startActivity(intent);
//                            finish();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                codeScanner.startPreview();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }
}