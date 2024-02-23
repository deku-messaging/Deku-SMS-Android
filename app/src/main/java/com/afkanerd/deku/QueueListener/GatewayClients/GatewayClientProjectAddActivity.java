package com.afkanerd.deku.QueueListener.GatewayClients;

import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_ID;
import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.room.Room;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsHandler;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GatewayClientProjectAddActivity extends AppCompatActivity {

    public static final String GATEWAY_CLIENT_PROJECT_ID = "GATEWAY_CLIENT_PROJECT_ID";
    GatewayClient gatewayClient;
    GatewayClientHandler gatewayClientHandler;

    SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    Toolbar toolbar;

    Datastore databaseConnector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_customization);

        toolbar = findViewById(R.id.gateway_client_customization_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        databaseConnector = Room.databaseBuilder(getApplicationContext(),
                Datastore.class, Datastore.databaseName).build();

        try {
            getGatewayClient();
            getSupportActionBar().setTitle(gatewayClient == null ?
                    getString(R.string.add_new_gateway_server_toolbar_title) :
                    gatewayClient.getHostUrl());
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

    long id = -1;
    private void getGatewayClient() throws InterruptedException {
        TextInputEditText projectName = findViewById(R.id.new_gateway_client_project_name);
        TextInputEditText projectBinding = findViewById(R.id.new_gateway_client_project_binding_sim_1);
        TextInputEditText projectBinding2 = findViewById(R.id.new_gateway_client_project_binding_sim_2);

        gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        long gatewayId = getIntent().getLongExtra(GATEWAY_CLIENT_ID, -1);
        gatewayClient = gatewayClientHandler.fetch(gatewayId);

        final boolean isDualSim = SIMHandler.isDualSim(getApplicationContext());
        if(isDualSim) {
            findViewById(R.id.new_gateway_client_project_binding_sim_2_constraint)
                    .setVisibility(View.VISIBLE);
        }

        if(getIntent().hasExtra(GATEWAY_CLIENT_PROJECT_ID)) {
            id = getIntent().getLongExtra(GATEWAY_CLIENT_PROJECT_ID, -1);
            consumerExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    GatewayClientProjects gatewayClientProjects =
                            databaseConnector.gatewayClientProjectDao().fetch(id);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            projectName.setText(gatewayClientProjects.name);
                            projectBinding.setText(gatewayClientProjects.binding1Name);
                        }
                    });

                    if (isDualSim) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                projectBinding2.setText(gatewayClientProjects.binding2Name);
                            }
                        });
                    }
                }
            });
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

        if(id == -1) {
            GatewayClientProjects gatewayClientProjects = new GatewayClientProjects();
            gatewayClientProjects.name = projectName.getText().toString();
            gatewayClientProjects.binding1Name = projectBinding.getText().toString();
            gatewayClientProjects.binding2Name = projectBinding2.getText().toString();
            gatewayClientProjects.gatewayClientId = gatewayClient.getId();

            consumerExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayClientProjectDao().insert(gatewayClientProjects);
                }
            });
        }
        else {
            consumerExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    GatewayClientProjects gatewayClientProjects =
                            databaseConnector.gatewayClientProjectDao().fetch(id);
                    gatewayClientProjects.name = projectName.getText().toString();
                    gatewayClientProjects.binding1Name = projectBinding.getText().toString();
                    gatewayClientProjects.binding2Name = projectBinding2.getText().toString();
                    gatewayClientProjects.gatewayClientId = gatewayClient.getId();
                    databaseConnector.gatewayClientProjectDao().update(gatewayClientProjects);
                }
            });
        }

        Intent intent = new Intent(this, GatewayClientListingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getIntent().hasExtra(GATEWAY_CLIENT_PROJECT_ID))
            getMenuInflater().inflate(R.menu.gateway_client_customization_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    ExecutorService consumerExecutorService = Executors.newFixedThreadPool(2); // Create a pool of 5 worker threads
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.gateway_client_project_delete == item.getItemId()) {
            consumerExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayClientProjectDao().delete(id);
                    finish();
                }
            });
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}