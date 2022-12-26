package com.example.swob_deku;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.GatewayServer.GatewayServerDAO;
import com.example.swob_deku.Models.GatewayServer.GatewayServerHandler;
import com.example.swob_deku.Models.GatewayServer.GatewayServerRecyclerAdapter;
import com.example.swob_deku.Models.GatewayServer.GatewayServerViewModel;

import java.util.ArrayList;
import java.util.List;

public class GatewayServerListingActivity extends AppCompatActivity {
    Datastore databaseConnector;
    GatewayServerDAO gatewayServerDAO;

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

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        RecyclerView recentsRecyclerView = findViewById(R.id.gateway_server_listing_recycler_view);
        recentsRecyclerView.setLayoutManager(linearLayoutManager);

        GatewayServerRecyclerAdapter gatewayServerRecyclerAdapter = new GatewayServerRecyclerAdapter(this,
                R.layout.layout_gateway_server_list);
        recentsRecyclerView.setAdapter(gatewayServerRecyclerAdapter);

        GatewayServerViewModel gatewayServerViewModel = new ViewModelProvider(this).get(
                GatewayServerViewModel.class);

//        gatewayServerViewModel.getGatewayServers(gatewayServerDAO).observe(this,
//                list -> gatewayServerRecyclerAdapter.submitList(list));

        databaseConnector = Room.databaseBuilder(getApplicationContext(), Datastore.class,
                Datastore.databaseName).build();

        gatewayServerDAO = databaseConnector.gatewayServerDAO();

        gatewayServerViewModel.getGatewayServers(gatewayServerDAO).observe(this,
                new Observer<List<GatewayServer>>() {
                    @Override
                    public void onChanged(List<GatewayServer> gatewayServerList) {
                        Log.d(getLocalClassName(), "Changed happening....");
                        if(gatewayServerList.size() < 1 )
                            findViewById(R.id.no_gateway_server_added).setVisibility(View.VISIBLE);
                        gatewayServerRecyclerAdapter.submitList(gatewayServerList);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gateway_client_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
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

    @Override
    protected void onResume() {
        super.onResume();
    }
}

