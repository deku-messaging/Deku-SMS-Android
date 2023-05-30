package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;

import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayClientAddActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_add);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.new_gateway_client_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
    }

    public void onSaveGatewayClient(View view) throws InterruptedException {
        TextInputEditText url = findViewById(R.id.new_gateway_client_url_input);
        TextInputEditText username = findViewById(R.id.new_gateway_client_username);
        TextInputEditText password = findViewById(R.id.new_gateway_password);
        TextInputEditText friendlyName = findViewById(R.id.new_gateway_client_friendly_name);
        TextInputEditText virtualHost = findViewById(R.id.new_gateway_client_virtualhost);
        TextInputEditText port = findViewById(R.id.new_gateway_client_port);

        if(url.getText().toString().isEmpty()) {
            url.setError(getResources().getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }
        if(username.getText().toString().isEmpty()) {
            username.setError(getResources().getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }
        if(password.getText().toString().isEmpty()) {
            password.setError(getResources().getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }
        if(virtualHost.getText().toString().isEmpty()) {
            virtualHost.setText(getResources().getString(R.string.settings_gateway_client_default_virtualhost));
        }
        if(port.getText().toString().isEmpty()) {
            port.setText(getResources().getString(R.string.settings_gateway_client_default_port));
        }

        GatewayClient gatewayClient = new GatewayClient();
        gatewayClient.setHostUrl(url.getText().toString());
        gatewayClient.setUsername(username.getText().toString());
        gatewayClient.setPassword(password.getText().toString());
        gatewayClient.setVirtualHost(virtualHost.getText().toString());
        gatewayClient.setPort(Integer.parseInt(port.getText().toString()));

        if(!friendlyName.getText().toString().isEmpty()) {
            gatewayClient.setFriendlyConnectionName(friendlyName.getText().toString());
        }

        GatewayClientHandler.add(getApplicationContext(), gatewayClient);
        finish();
    }
}