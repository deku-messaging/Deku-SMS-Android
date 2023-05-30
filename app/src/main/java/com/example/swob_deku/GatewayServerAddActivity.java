package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import com.example.swob_deku.Models.GatewayServers.GatewayServer;
import com.example.swob_deku.Models.GatewayServers.GatewayServerHandler;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayServerAddActivity extends AppCompatActivity {
    MaterialCheckBox all, base64;

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

        dataTypeFilter();
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

        String formats = "";
        String protocol = GatewayServer.POST_PROTOCOL;

        if(base64.isChecked())
            formats = GatewayServer.BASE64_FORMAT;

        RadioGroup radioGroup = findViewById(R.id.add_gateway_server_protocol_group);
        int checkedRadioId = radioGroup.getCheckedRadioButtonId();
        if(checkedRadioId == R.id.add_gateway_protocol_GET)
            protocol = GatewayServer.GET_PROTOCOL;

        // TODO: test if valid url
        GatewayServer gatewayServer = new GatewayServer(gatewayServerUrl);
        gatewayServer.setFormat(formats);
        gatewayServer.setProtocol(protocol);

        try {
            GatewayServerHandler.add(getApplicationContext(), gatewayServer);
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            Intent gatewayServerListIntent = new Intent(this, GatewayServerListingActivity.class);
            gatewayServerListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(gatewayServerListIntent);
        }
    }
}