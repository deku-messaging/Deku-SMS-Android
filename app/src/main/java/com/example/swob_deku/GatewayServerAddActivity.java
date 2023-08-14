package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import com.example.swob_deku.Models.GatewayServers.GatewayServer;
import com.example.swob_deku.Models.GatewayServers.GatewayServerDAO;
import com.example.swob_deku.Models.GatewayServers.GatewayServerHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayServerAddActivity extends AppCompatActivity {
    MaterialCheckBox all, base64;

    GatewayServerHandler gatewayServerHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_server_add);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.new_gateway_server_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        gatewayServerHandler = new GatewayServerHandler(getApplicationContext());

        dataTypeFilter();

        MaterialButton materialButton = findViewById(R.id.gateway_client_customization_save_btn);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveGatewayServer(v);
            }
        });

        populateForUpdates();
    }

    private void populateForUpdates() {
        TextInputEditText textInputEditTextUrl = findViewById(R.id.new_gateway_client_url_input);
        RadioGroup radioGroup = findViewById(R.id.add_gateway_server_protocol_group);

        TextInputEditText textInputEditTextTag = findViewById(R.id.new_gateway_client_tag_input);

        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_URL)) {
            textInputEditTextUrl.setText(getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_URL));
        }

        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_TAG)) {
            textInputEditTextTag.setText(getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_TAG));
        }

        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_FORMAT)) {
            String format = getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_FORMAT);
            if(format.equals(GatewayServer.BASE64_FORMAT))
                base64.setChecked(true);
        }

    }

    private void dataTypeFilter(){
        all = findViewById(R.id.add_gateway_data_format_all);
        base64 = findViewById(R.id.add_gateway_data_format_base64);

        all.addOnCheckedStateChangedListener(new MaterialCheckBox.OnCheckedStateChangedListener() {
            @Override
            public void onCheckedStateChangedListener(@NonNull MaterialCheckBox checkBox, int state) {
                if(state == 1) {
                    base64.setChecked(false);
                }
            }
        });
        base64.addOnCheckedStateChangedListener(new MaterialCheckBox.OnCheckedStateChangedListener() {
            @Override
            public void onCheckedStateChangedListener(@NonNull MaterialCheckBox checkBox, int state) {
                if(state == 1) {
                    all.setChecked(false);
                }
            }
        });
    }

    public void onSaveGatewayServer(View view) {
        TextInputEditText textInputEditTextUrl = findViewById(R.id.new_gateway_client_url_input);
        String gatewayServerUrl = textInputEditTextUrl.getText().toString();

        TextInputEditText textInputEditTextTag = findViewById(R.id.new_gateway_client_tag_input);
        String gatewayServerTag = textInputEditTextTag.getText().toString();

        String formats = "";
        String protocol = GatewayServer.POST_PROTOCOL;

        if(base64.isChecked())
            formats = GatewayServer.BASE64_FORMAT;

        RadioGroup radioGroup = findViewById(R.id.add_gateway_server_protocol_group);
        int checkedRadioId = radioGroup.getCheckedRadioButtonId();
        if(checkedRadioId == R.id.add_gateway_protocol_GET)
            protocol = GatewayServer.GET_PROTOCOL;

        // Important: test if valid url
        GatewayServer gatewayServer = new GatewayServer(gatewayServerUrl);
        gatewayServer.setTag(gatewayServerTag);
        gatewayServer.setFormat(formats);
        gatewayServer.setProtocol(protocol);

        try {
            if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_ID)) {
                gatewayServer.setId(getIntent().getLongExtra(GatewayServer.GATEWAY_SERVER_ID, -1));
                gatewayServerHandler.update(gatewayServer);
            }
            else
                gatewayServerHandler.add(gatewayServer);

            Intent gatewayServerListIntent = new Intent(this, GatewayServerListingActivity.class);
            gatewayServerListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(gatewayServerListIntent);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_ID)) {
            getMenuInflater().inflate(R.menu.gateway_server_add_menu, menu);
            return super.onCreateOptionsMenu(menu);
        }
        return false;
    }

    private void deleteGatewayServer() throws InterruptedException {
        gatewayServerHandler.delete(getIntent().getLongExtra(GatewayServer.GATEWAY_SERVER_ID, -1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gateway_client_delete) {
            try {
                deleteGatewayServer();

                Intent gatewayServerListIntent = new Intent(this, GatewayServerListingActivity.class);
                gatewayServerListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(gatewayServerListIntent);

                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        gatewayServerHandler.close();

        super.onDestroy();
    }
}