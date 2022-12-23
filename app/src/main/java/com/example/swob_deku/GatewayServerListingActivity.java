package com.example.swob_deku;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.GatewayServer.GatewayServerHandler;
import com.example.swob_deku.Models.GatewayServer.GatewayServerRecyclerAdapter;

import java.util.List;

public class GatewayServerListingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_servers_listing_activitiy);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.gateway_server_listing_toolbar);
        myToolbar.setTitle(R.string.gateway_server_listing_toolbar_title);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gateway_client_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        populateGatewayServers();
    }

    public void populateGatewayServers() {
        RecyclerView recentsRecyclerView = findViewById(R.id.gateway_server_listing_recycler_view);
        // recentsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        try {
            List<GatewayServer> gatewayServerList = GatewayServerHandler.fetchAll(getApplicationContext());

            if(gatewayServerList.size() < 1 ) {
                findViewById(R.id.no_gateway_server_added).setVisibility(View.VISIBLE);
                return;
            }

            GatewayServerRecyclerAdapter gatewayServerRecyclerAdapter = new GatewayServerRecyclerAdapter(this,
                    gatewayServerList, R.layout.layout_gateway_server_list);

            recentsRecyclerView.setAdapter(gatewayServerRecyclerAdapter);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            recentsRecyclerView.setLayoutManager(linearLayoutManager);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.add_gateway_server:
                Intent addGatewayIntent = new Intent(getApplicationContext(), GatewayServerAddActivity.class);
                startActivity(addGatewayIntent);
                break;
        }
        return false;
    }
}