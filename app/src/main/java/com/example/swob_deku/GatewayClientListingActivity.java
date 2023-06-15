package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayClients.GatewayClient;
import com.example.swob_deku.Models.GatewayClients.GatewayClientDAO;
import com.example.swob_deku.Models.GatewayClients.GatewayClientRecyclerAdapter;
import com.example.swob_deku.Models.GatewayClients.GatewayClientViewModel;

import java.util.List;

public class GatewayClientListingActivity extends AppCompatActivity {

    public static String GATEWAY_CLIENT_ID = "GATEWAY_CLIENT_ID";
    public static String GATEWAY_CLIENT_USERNAME = "GATEWAY_CLIENT_USERNAME";
    public static String GATEWAY_CLIENT_PASSWORD = "GATEWAY_CLIENT_PASSWORD";
    public static String GATEWAY_CLIENT_VIRTUAL_HOST = "GATEWAY_CLIENT_VIRTUAL_HOST";
    public static String GATEWAY_CLIENT_HOST = "GATEWAY_CLIENT_HOST";
    public static String GATEWAY_CLIENT_PORT = "GATEWAY_CLIENT_PORT";
    public static String GATEWAY_CLIENT_FRIENDLY_NAME = "GATEWAY_CLIENT_FRIENDLY_NAME";

    public static String GATEWAY_CLIENT_LISTENERS = "GATEWAY_CLIENT_LISTENERS";
    public static String GATEWAY_CLIENT_STOP_LISTENERS = "GATEWAY_CLIENT_STOP_LISTENERS";

    SharedPreferences sharedPreferences;
    Datastore databaseConnector;

    GatewayClientDAO gatewayClientDAO;

    Handler mHandler = new Handler();

    GatewayClientRecyclerAdapter gatewayClientRecyclerAdapter;

    GatewayClientViewModel gatewayClientViewModel;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_listing);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.gateway_client_listing_toolbar);
        myToolbar.setTitle(R.string.gateway_client_listing_toolbar_title);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        RecyclerView recyclerView = findViewById(R.id.gateway_client_listing_recycler_view);
        recyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getApplicationContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        gatewayClientRecyclerAdapter = new GatewayClientRecyclerAdapter(this);
        recyclerView.setAdapter(gatewayClientRecyclerAdapter);

        gatewayClientViewModel = new ViewModelProvider(this).get(
                GatewayClientViewModel.class);

        databaseConnector = Room.databaseBuilder(getApplicationContext(), Datastore.class,
                Datastore.databaseName).build();

        gatewayClientDAO = databaseConnector.gatewayClientDAO();

        gatewayClientViewModel.getGatewayClientList(
                getApplicationContext(), gatewayClientDAO).observe(this,
                new Observer<List<GatewayClient>>() {
                    @Override
                    public void onChanged(List<GatewayClient> gatewayServerList) {
                        if(gatewayServerList.size() < 1 )
                            findViewById(R.id.gateway_client_no_gateway_client_label).setVisibility(View.VISIBLE);
                        gatewayClientRecyclerAdapter.submitList(gatewayServerList);
                    }
                });

        registerListeners();

        setRefreshTimer(gatewayClientRecyclerAdapter);
    }

    private void registerListeners() {
        sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                gatewayClientViewModel.refresh(getApplicationContext());
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void setRefreshTimer(GatewayClientRecyclerAdapter adapter) {
        final int recyclerViewTimeUpdateLimit = 60 * 1000;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                mHandler.postDelayed(this, recyclerViewTimeUpdateLimit);
            }
        }, recyclerViewTimeUpdateLimit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gateway_client_add_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.add_gateway_server:
                Intent addGatewayIntent = new Intent(getApplicationContext(), GatewayClientAddActivity.class);
                startActivity(addGatewayIntent);
                break;
        }
        return false;
    }

    private boolean saveListenerConfiguration(int id) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        return editor.putLong(String.valueOf(id), System.currentTimeMillis())
                .commit();
    }

    private boolean removeListenerConfiguration(int id) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        return editor.remove(String.valueOf(id))
                        .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseConnector.close();
    }
}