package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;

import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.GatewayServer.GatewayServerHandler;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayServerAddActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_server_add);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.new_gateway_client_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
    }

    public void onSaveGatewayServer(View view) {
        TextInputEditText textInputEditTextUrl = findViewById(R.id.new_gateway_client_url_input);
        String gatewayServerUrl = textInputEditTextUrl.getText().toString();

        // TODO: test if valid url

        GatewayServer gatewayServer = new GatewayServer(gatewayServerUrl);
        try {
            GatewayServerHandler.add(getApplicationContext(), gatewayServer);
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            finish();
        }

        // TODO: add logs to debug mode
    }
}