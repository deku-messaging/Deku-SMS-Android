package com.afkanerd.deku.Router.Router;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouterActivity extends CustomAppCompactActivity {

    RouterViewModel routerViewModel;
    RecyclerView routedMessageRecyclerView;
    RouterRecyclerAdapter routerRecyclerAdapter;

    ActionMode actionMode;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        toolbar = findViewById(R.id.router_activity_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.settings_SMS_routing_title));

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager);

        routerRecyclerAdapter = new RouterRecyclerAdapter();
        routerRecyclerAdapter.setHasStableIds(true);

        routedMessageRecyclerView.setAdapter(routerRecyclerAdapter);

        routerViewModel = new ViewModelProvider(this).get( RouterViewModel.class);

        routerViewModel.getMessages(getApplicationContext()).observe(this,
                new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(List<WorkInfo> workInfoList) {
                        routerRecyclerAdapter.mDiffer.submitList(workInfoList);
                        if(!workInfoList.isEmpty())
                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.GONE);
                        else {
                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.VISIBLE);
                            routedMessageRecyclerView.smoothScrollToPosition(0);
                        }
                    }
                });

        routerRecyclerAdapter.selectedItems.observe(this, new Observer<HashMap<Long, RouterRecyclerAdapter.ViewHolder>>() {
            @Override
            public void onChanged(HashMap<Long, RouterRecyclerAdapter.ViewHolder> longs) {
                if(longs == null || longs.isEmpty()) {
                    if(actionMode != null) {
                        actionMode.finish();
                    }
                    return;
                }
                else if(actionMode == null) {
                    actionMode = startActionMode(actionModeCallback);
                }
                if(actionMode != null) actionMode.setTitle(String.valueOf(longs.size()));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.gateway_server_routed_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.router_list_gateways_menu_item) {
            startActivity(new Intent(this, GatewayServerListingActivity.class));
            return true;
        }
        return false;
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.gateway_server_routed_menu_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            routerRecyclerAdapter.resetAllSelected();
        }
    };
}