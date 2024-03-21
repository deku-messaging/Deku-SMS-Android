package com.afkanerd.deku.Router.GatewayServers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.Router.RouterHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

public class GatewayServerAddActivity extends AppCompatActivity {

    GatewayServerHandler gatewayServerHandler;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_modalsheet_gateway_server_http_add_layout);

//        toolbar = findViewById(R.id.gateway_server_add_toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setTitle(R.string.add_new_gateway_server_toolbar_title);

        gatewayServerHandler = new GatewayServerHandler(getApplicationContext());

//        dataTypeFilter();

//        populateForUpdates();
    }

//    private void populateForUpdates() {
//        TextInputEditText textInputEditTextUrl = findViewById(R.id.new_gateway_server_url_input);
//        TextInputEditText textInputEditTextTag = findViewById(R.id.new_gateway_server_tag_input);
//
//        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_URL)) {
//            textInputEditTextUrl.setText(getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_URL));
//        }
//
//        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_TAG)) {
//            textInputEditTextTag.setText(getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_TAG));
//        }
//
//        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_FORMAT)) {
//            String format = getIntent().getStringExtra(GatewayServer.GATEWAY_SERVER_FORMAT);
//            if(format != null && format.equals(GatewayServer.BASE64_FORMAT))
//                base64.setChecked(true);
//        }
//
//    }

//    private void dataTypeFilter(){
//        all = findViewById(R.id.add_gateway_data_format_all);
//        base64 = findViewById(R.id.add_gateway_data_format_base64);
//
//        all.addOnCheckedStateChangedListener(new MaterialCheckBox.OnCheckedStateChangedListener() {
//            @Override
//            public void onCheckedStateChangedListener(@NonNull MaterialCheckBox checkBox, int state) {
//                if(state == 1) {
//                    base64.setChecked(false);
//                }
//            }
//        });
//        base64.addOnCheckedStateChangedListener(new MaterialCheckBox.OnCheckedStateChangedListener() {
//            @Override
//            public void onCheckedStateChangedListener(@NonNull MaterialCheckBox checkBox, int state) {
//                if(state == 1) {
//                    all.setChecked(false);
//                }
//            }
//        });
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_ID)) {
            getMenuInflater().inflate(R.menu.gateway_server_add_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void deleteGatewayServer() throws InterruptedException {
        gatewayServerHandler.delete(getIntent().getLongExtra(GatewayServer.GATEWAY_SERVER_ID, -1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gateway_client_delete) {
            try {
                long gatewayServerId = getIntent().getLongExtra(GatewayServer.GATEWAY_SERVER_ID, -1);
                GatewayServer gatewayServer = gatewayServerHandler.get(gatewayServerId);

                deleteGatewayServer();
                RouterHandler.removeWorkForGatewayServers(getApplicationContext(), gatewayServer.getURL());

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
}