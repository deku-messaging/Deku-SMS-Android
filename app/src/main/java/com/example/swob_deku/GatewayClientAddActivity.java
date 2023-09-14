package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.google.android.material.button.MaterialButton;
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

        if(getIntent().hasExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID)) {
            try {
                editGatewayClient();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        MaterialButton materialButton = findViewById(R.id.gateway_client_customization_save_btn);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onSaveGatewayClient(v);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void editGatewayClient() throws InterruptedException {
        long gatewayClientId = getIntent().getLongExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1);

        if(gatewayClientId != -1 ) {
            TextInputEditText url = findViewById(R.id.new_gateway_client_url_input);
            TextInputEditText username = findViewById(R.id.new_gateway_client_username);
            TextInputEditText password = findViewById(R.id.new_gateway_password);
            TextInputEditText friendlyName = findViewById(R.id.new_gateway_client_friendly_name);
            TextInputEditText virtualHost = findViewById(R.id.new_gateway_client_virtualhost);
            TextInputEditText port = findViewById(R.id.new_gateway_client_port);

            GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
            GatewayClient gatewayClient = gatewayClientHandler.fetch(gatewayClientId);

            url.setText(gatewayClient.getHostUrl());
            username.setText(gatewayClient.getUsername());
            password.setText(gatewayClient.getPassword());
            friendlyName.setText(gatewayClient.getFriendlyConnectionName());
            virtualHost.setText(gatewayClient.getVirtualHost());
            port.setText(String.valueOf(gatewayClient.getPort()));
            gatewayClientHandler.close();
        }
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
            password.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
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
        gatewayClient.setDate(System.currentTimeMillis());

        if(!friendlyName.getText().toString().isEmpty()) {
            gatewayClient.setFriendlyConnectionName(friendlyName.getText().toString());
        }

        RadioGroup radioGroup = findViewById(R.id.add_gateway_client_protocol_group);
        int checkedRadioId = radioGroup.getCheckedRadioButtonId();
        if(checkedRadioId == R.id.add_gateway_client_protocol_amqp)
            gatewayClient.setProtocol(getString(R.string.settings_gateway_client_amqp_protocol).toLowerCase());

        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        if(getIntent().hasExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID)) {
            int gatewayClientId = getIntent().getIntExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1);

            GatewayClient gatewayClient1 = gatewayClientHandler.fetch(gatewayClientId);

            gatewayClient.setId(gatewayClient1.getId());
            gatewayClient.setProjectName(gatewayClient1.getProjectName());
            gatewayClient.setProjectBinding(gatewayClient1.getProjectBinding());
            gatewayClientHandler.update(gatewayClient);
        }
        else {
            gatewayClientHandler.add(gatewayClient);
        }
        gatewayClientHandler.close();

        Intent intent = new Intent(this, GatewayClientListingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}