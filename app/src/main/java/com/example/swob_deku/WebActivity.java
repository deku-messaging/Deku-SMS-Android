package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.example.swob_deku.Models.GatewayServers.GatewayServer;
import com.example.swob_deku.Models.GatewayServers.GatewayServerHandler;
import com.google.zxing.Result;

import org.json.JSONObject;

public class WebActivity extends AppCompatActivity {

    private CodeScanner codeScanner;

    public final String WEB_LINKED_URL = "url";
    public final String WEB_LINKED_TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

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
                            String resultValue = result.getText();

                            JSONObject jsonObject = new JSONObject(resultValue);
                            String url = jsonObject.getString(WEB_LINKED_URL);
                            String tag = jsonObject.getString(WEB_LINKED_TAG);

//                            Toast.makeText(getApplicationContext(),
//                                    "url: " + url + "\ntag: " + tag, Toast.LENGTH_SHORT).show();
                            GatewayServer gatewayServer = new GatewayServer();
                            gatewayServer.setTag(tag);
                            gatewayServer.setURL(url);

                            GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(getApplicationContext());
                            gatewayServerHandler.add(gatewayServer);

                            Toast.makeText(getApplicationContext(), "New Gateway client added!", Toast.LENGTH_SHORT)
                                    .show();
                            startActivity(new Intent(getApplicationContext(), GatewayServerListingActivity.class));
                            finish();
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