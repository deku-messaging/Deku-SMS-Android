package com.afkanerd.deku.Router.GatewayServers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.Fragments.ModalSheetFragment;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.List;

public class GatewayServerListingActivity extends AppCompatActivity {
    Handler mHandler = new Handler();

    SharedPreferences sharedPreferences;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_servers_listing_activitiy);

        toolbar = findViewById(R.id.gateway_servers_listing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.gateway_server_listing_toolbar_title));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        RecyclerView recentsRecyclerView = findViewById(R.id.gateway_server_listing_recycler_view);
        recentsRecyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getApplicationContext(),
                linearLayoutManager.getOrientation());
        recentsRecyclerView.addItemDecoration(dividerItemDecoration);

        GatewayServerRecyclerAdapter gatewayServerRecyclerAdapter = new GatewayServerRecyclerAdapter(this);
        recentsRecyclerView.setAdapter(gatewayServerRecyclerAdapter);

        GatewayServerViewModel gatewayServerViewModel = new ViewModelProvider(this).get(
                GatewayServerViewModel.class);

        try {
            gatewayServerViewModel.get(getApplicationContext()).observe(this,
                    new Observer<List<GatewayServer>>() {
                        @Override
                        public void onChanged(List<GatewayServer> gatewayServerList) {
                            Log.d(getLocalClassName(), "Changed happening....");
                            if(gatewayServerList.size() < 1 )
                                findViewById(R.id.no_gateway_server_added).setVisibility(View.VISIBLE);
                            gatewayServerRecyclerAdapter.submitList(gatewayServerList);
                        }
                    });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setRefreshTimer(gatewayServerRecyclerAdapter);
        showSecureRequestAgreementModal();
    }


    private void setRefreshTimer(GatewayServerRecyclerAdapter adapter) {
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
        getMenuInflater().inflate(R.menu.gateway_client_listing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gateway_client_add_manually) {
            Intent addGatewayIntent = new Intent(getApplicationContext(), GatewayServerAddActivity.class);
            startActivity(addGatewayIntent);
            return true;
        }
        return false;
    }

    private void showSecureRequestAgreementModal() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(GatewayServerAddModelFragment.TAG);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        GatewayServerAddModelFragment gatewayServerAddModelFragment =
                new GatewayServerAddModelFragment();
        fragmentTransaction.add(gatewayServerAddModelFragment,
                ModalSheetFragment.TAG);
        fragmentTransaction.show(gatewayServerAddModelFragment);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentTransaction.commitNow();
//                modalSheetFragment.getView().findViewById(R.id.conversation_secure_request_agree_btn)
//                        .setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                modalSheetFragment.dismiss();
//                                agreeToSecure();
//                            }
//                        });
            }
        });
    }
}

