package com.example.swob_deku;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
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
            List<GatewayServer> encryptedContentList = GatewayServerHandler.fetchAll(getApplicationContext());

            GatewayServerRecyclerAdapter gatewayServerRecyclerAdapter = new GatewayServerRecyclerAdapter(this,
                    encryptedContentList, R.layout.layout_gateway_server_list);

            recentsRecyclerView.setAdapter(gatewayServerRecyclerAdapter);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            recentsRecyclerView.setLayoutManager(linearLayoutManager);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}