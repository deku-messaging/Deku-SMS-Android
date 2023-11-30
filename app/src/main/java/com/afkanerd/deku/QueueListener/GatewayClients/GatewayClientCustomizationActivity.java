package com.afkanerd.deku.QueueListener.GatewayClients;

import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_ID;
import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class GatewayClientCustomizationActivity extends AppCompatActivity {

    GatewayClient gatewayClient;
    GatewayClientHandler gatewayClientHandler;

    SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_customization);

        toolbar = findViewById(R.id.gateway_client_customization_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            getGatewayClient();
            getSupportActionBar().setTitle(gatewayClient.getHostUrl());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                invalidateOptionsMenu();
            }
        };

        sharedPreferences = getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

//        checkForBatteryOptimization();

        MaterialButton materialButton = findViewById(R.id.gateway_client_customization_save_btn);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onSaveGatewayClientConfiguration(v);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void checkForBatteryOptimization() {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        startActivity(intent);
    }

    private void getGatewayClient() throws InterruptedException {
        TextInputEditText projectName = findViewById(R.id.new_gateway_client_project_name);
        TextInputEditText projectBinding = findViewById(R.id.new_gateway_client_project_binding_sim_1);
        TextInputEditText projectBinding2 = findViewById(R.id.new_gateway_client_project_binding_sim_2);

        gatewayClientHandler = new GatewayClientHandler(getApplicationContext());

        long gatewayId = getIntent().getLongExtra(GATEWAY_CLIENT_ID, -1);
        gatewayClient = gatewayClientHandler.fetch(gatewayId);

        if(gatewayClient.getProjectName() != null && !gatewayClient.getProjectName().isEmpty())
            projectName.setText(gatewayClient.getProjectName());

        if(gatewayClient.getProjectBinding() != null && !gatewayClient.getProjectBinding().isEmpty())
            projectBinding.setText(gatewayClient.getProjectBinding());

        List<SubscriptionInfo> simcards = SIMHandler.getSimCardInformation(getApplicationContext());
        if(simcards.size() > 1) {
            findViewById(R.id.new_gateway_client_project_binding_sim_2_constraint).setVisibility(View.VISIBLE);
            if(gatewayClient.getProjectBinding2() != null && !gatewayClient.getProjectBinding2().isEmpty())
                projectBinding2.setText(gatewayClient.getProjectBinding2());
        }

        projectName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                List<String> projectBindings = GatewayClientHandler.getPublisherDetails(getApplicationContext(),
                        s.toString());

                projectBinding.setText(projectBindings.get(0));

                if(projectBindings.size() > 1) {
                    projectBinding2.setText(projectBindings.get(1));
                }
            }
        });

    }

    public void onSaveGatewayClientConfiguration(View view) throws InterruptedException {
        TextInputEditText projectName = findViewById(R.id.new_gateway_client_project_name);

        TextInputEditText projectBinding = findViewById(R.id.new_gateway_client_project_binding_sim_1);
        TextInputEditText projectBinding2 = findViewById(R.id.new_gateway_client_project_binding_sim_2);
        ConstraintLayout projectBindingConstraint = findViewById(R.id.new_gateway_client_project_binding_sim_2_constraint);

        if(projectName.getText() == null || projectName.getText().toString().isEmpty()) {
            projectName.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }

        if(projectBinding.getText() == null || projectBinding.getText().toString().isEmpty()) {
            projectBinding.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }

        if(projectBindingConstraint.getVisibility() == View.VISIBLE &&
                (projectBinding2.getText() == null || projectBinding2.getText().toString().isEmpty())) {
            projectBinding2.setError(getString(R.string.settings_gateway_client_cannot_be_empty));
            return;
        }

        gatewayClient.setProjectName(projectName.getText().toString());
        gatewayClient.setProjectBinding(projectBinding.getText().toString());

        if(projectBinding2.getVisibility() == View.VISIBLE && projectBinding2.getText() != null)
            gatewayClient.setProjectBinding2(projectBinding2.getText().toString());

        gatewayClientHandler.update(gatewayClient);

        Intent intent = new Intent(this, GatewayClientListingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean connected = sharedPreferences.contains(String.valueOf(gatewayClient.getId()));
        getMenuInflater().inflate(R.menu.gateway_client_customization_menu, menu);
        menu.findItem(R.id.gateway_client_connect).setVisible(!connected);
        menu.findItem(R.id.gateway_client_disconnect).setVisible(connected);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.gateway_client_delete:
                try {
                    deleteGatewayClient();
                    return true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.gateway_client_connect:
                try {
                    GatewayClientHandler.startListening(getApplicationContext(), gatewayClient);
                    return true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.gateway_client_disconnect:
                stopListening();
                return true;

            case R.id.gateway_client_edit:
                editGatewayClient();
                return true;
        }
        return false;
    }

    private void editGatewayClient() {
        Intent intent = new Intent(this, GatewayClientAddActivity.class);
        intent.putExtra(GATEWAY_CLIENT_ID, gatewayClient.getId());

        startActivity(intent);
    }

    public void stopListening() {
        sharedPreferences.edit().remove(String.valueOf(gatewayClient.getId()))
                .apply();
    }

    private void deleteGatewayClient() throws InterruptedException {
        stopListening();
        gatewayClientHandler.delete(gatewayClient);
        startActivity(new Intent(this, GatewayClientListingActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gatewayClientHandler.close();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}