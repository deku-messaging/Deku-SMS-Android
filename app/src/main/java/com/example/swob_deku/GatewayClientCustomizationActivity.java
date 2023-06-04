package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayClientCustomizationActivity extends AppCompatActivity {

    GatewayClient gatewayClient;
    GatewayClientHandler gatewayClientHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_customization);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.gateway_client_customization_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        try {
            getGatewayClient();
            ab.setTitle(gatewayClient.getHostUrl());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void getGatewayClient() throws InterruptedException {
        gatewayClientHandler = new GatewayClientHandler(getApplicationContext());

        int gatewayId = getIntent().getIntExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1);
        gatewayClient = gatewayClientHandler.fetch(gatewayId);
    }

    public void onSaveGatewayClientConfiguration(View view) throws InterruptedException {
        TextInputEditText projectName = findViewById(R.id.new_gateway_client_project_name);
        TextInputEditText projectBinding = findViewById(R.id.new_gateway_client_project_binding);

        if(projectName.getText() == null || projectName.getText().toString().isEmpty()) {
            projectName.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }
        if(projectBinding.getText() == null || projectBinding.getText().toString().isEmpty()) {
            projectBinding.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }

        gatewayClient.setProjectName(projectName.getText().toString());
        gatewayClient.setProjectBinding(projectBinding.getText().toString());
        gatewayClientHandler.update(gatewayClient);

        Intent intent = new Intent(this, GatewayClientAddActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}