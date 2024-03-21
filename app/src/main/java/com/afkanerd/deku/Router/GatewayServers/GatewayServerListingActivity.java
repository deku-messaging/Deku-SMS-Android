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
import androidx.room.Room;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.Fragments.ModalSheetFragment;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class GatewayServerListingActivity extends AppCompatActivity {
    Handler mHandler = new Handler();

    SharedPreferences sharedPreferences;

    Toolbar toolbar;

    Datastore databaseConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_servers_listing_activitiy);

        if(Datastore.datastore == null || !Datastore.datastore.isOpen()) {
            Log.d(getClass().getName(), "Yes I am closed");
            Datastore.datastore = Room.databaseBuilder(getApplicationContext(), Datastore.class,
                            Datastore.databaseName)
                    .enableMultiInstanceInvalidation()
                    .build();
        }
        databaseConnector = Datastore.datastore;

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

    }

    MaterialCheckBox all, base64;
    public void onSaveGatewayServer(View view) {
        TextInputEditText textInputEditTextUrl = view.findViewById(R.id.new_gateway_server_url_input);
        String gatewayServerUrl = textInputEditTextUrl.getText().toString();

        TextInputEditText textInputEditTextTag = view.findViewById(R.id.new_gateway_server_tag_input);
        String gatewayServerTag = textInputEditTextTag.getText().toString();

        String formats = "";
        String protocol = GatewayServer.POST_PROTOCOL;

        GatewayServer gatewayServer = new GatewayServer(gatewayServerUrl);
//        if(base64.isChecked()) {
//            formats = GatewayServer.BASE64_FORMAT;
//        gatewayServer.setFormat(formats);
//    }

//        RadioGroup radioGroup = findViewById(R.id.add_gateway_server_protocol_group);
//        int checkedRadioId = radioGroup.getCheckedRadioButtonId();

        // Important: test if valid url
        gatewayServer.setTag(gatewayServerTag);
        gatewayServer.setProtocol(protocol);
        gatewayServer.setDate(System.currentTimeMillis());

        if(getIntent().hasExtra(GatewayServer.GATEWAY_SERVER_ID)) {
            gatewayServer.setId(getIntent().getLongExtra(GatewayServer.GATEWAY_SERVER_ID, -1));
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().update(gatewayServer);
                }
            });
        }
        else
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().insert(gatewayServer);
                }
            });

//            Intent gatewayServerListIntent = new Intent(this, GatewayServerListingActivity.class);
//            startActivity(gatewayServerListIntent);
//            finish();
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
        getMenuInflater().inflate(R.menu.gateway_server_listing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gateway_server_menu_add_http) {
            showSecureRequestAgreementModal(R.layout.fragment_modalsheet_gateway_server_http_add_layout);
            return true;
        }
        else if (item.getItemId() == R.id.gateway_server_menu_add_smtp) {
            showSecureRequestAgreementModal(R.layout.fragment_modalsheet_gateway_server_smtp_add_layout);
            return true;
        }
        return false;
    }

    private void showSecureRequestAgreementModal(int layout) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        GatewayServerAddModelFragment gatewayServerAddModelFragment =
                new GatewayServerAddModelFragment(layout);
        fragmentTransaction.add(gatewayServerAddModelFragment,
                ModalSheetFragment.TAG);
        fragmentTransaction.show(gatewayServerAddModelFragment);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentTransaction.commitNow();
                gatewayServerAddModelFragment.runnable = new Runnable() {
                    @Override
                    public void run() {
                        onSaveGatewayServer(gatewayServerAddModelFragment.getView());
                        gatewayServerAddModelFragment.dismiss();
                    }
                };
            }
        });
    }
}

