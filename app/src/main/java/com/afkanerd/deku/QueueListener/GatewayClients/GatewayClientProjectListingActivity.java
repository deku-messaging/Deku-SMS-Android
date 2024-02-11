package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.afkanerd.deku.DefaultSMS.R;

import java.util.List;

public class GatewayClientProjectListingActivity extends AppCompatActivity {

    long id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_client_project_listing);

        Toolbar toolbar = findViewById(R.id.gateway_client_project_listing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String username = getIntent().getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_USERNAME);
        String host = getIntent().getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_HOST);
        id = getIntent().getLongExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1);

        getSupportActionBar().setTitle(username);
        getSupportActionBar().setSubtitle(host);

        GatewayClientProjectListingRecyclerAdapter gatewayClientProjectListingRecyclerAdapter =
                new GatewayClientProjectListingRecyclerAdapter();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        RecyclerView recyclerView = findViewById(R.id.gateway_client_project_listing_recycler_view);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.setAdapter(gatewayClientProjectListingRecyclerAdapter);

        GatewayClientProjectListingViewModel gatewayClientProjectListingViewModel =
                new ViewModelProvider(this).get(GatewayClientProjectListingViewModel.class);

        gatewayClientProjectListingViewModel.get(getApplicationContext(), id).observe(this,
                new Observer<List<GatewayClientProjects>>() {
            @Override
            public void onChanged(List<GatewayClientProjects> gatewayClients) {
                gatewayClientProjectListingRecyclerAdapter.mDiffer.submitList(gatewayClients);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gateway_client_project_listing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.gateway_client_project_add) {
            Intent intent = new Intent(getApplicationContext(), GatewayClientCustomizationActivity.class);
            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, id);
            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID_NEW, true);
            startActivity(intent);
            return true;
        }
        return false;
    }
}