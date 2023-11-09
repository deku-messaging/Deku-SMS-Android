package com.afkanerd.deku.Router.Router;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerAddActivity;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerListingActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RouterActivity extends CustomAppCompactActivity {

    RouterViewModel routerViewModel;
    RecyclerView routedMessageRecyclerView;
    RouterRecyclerAdapter routerRecyclerAdapter;

    ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager);

        routerRecyclerAdapter = new RouterRecyclerAdapter(getApplicationContext());
        routerRecyclerAdapter.setHasStableIds(true);

        routedMessageRecyclerView.setAdapter(routerRecyclerAdapter);

        routerViewModel = new ViewModelProvider(this).get( RouterViewModel.class);

        routerViewModel.getMessages(getApplicationContext()).observe(this,
                new Observer<List<RouterItem>>() {
                    @Override
                    public void onChanged(List<RouterItem> smsList) {
                        routerRecyclerAdapter.submitList(smsList);
                        if(!smsList.isEmpty())
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
                actionMode.setTitle(String.valueOf(longs.size()));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.routing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.router_cancel_menu_item) {
        }
        if(item.getItemId() == R.id.router_list_gateways_menu_item) {
            startActivity(new Intent(this, GatewayServerListingActivity.class));
            return true;
        }
        if(item.getItemId() == R.id.router_add_gateways_menu_item) {
            startActivity(new Intent(this, GatewayServerAddActivity.class));
            return true;
        }
        return false;
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created. startActionMode() is called.
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items.
//            if(routerRecyclerAdapter.selectedItems != null &&
//                    routerRecyclerAdapter.selectedItems.getValue() != null &&
//                    !routerRecyclerAdapter.selectedItems.getValue().isEmpty()) {
//                mode.getMenuInflater().inflate(R.menu.routing_menu_items_selected, menu);
//            } else {
//                mode.getMenuInflater().inflate(R.menu.routing_menu, menu);
//            }
            mode.getMenuInflater().inflate(R.menu.routing_menu_items_selected, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, and might be called multiple times if the mode
        // is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        // Called when the user selects a contextual menu item.
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.router_cancel_menu_item) {
                if(routerRecyclerAdapter.selectedItems.getValue() != null) {
                    for (Map.Entry<Long, RouterRecyclerAdapter.ViewHolder>entry : routerRecyclerAdapter.selectedItems.getValue().entrySet()) {
                        RouterItem routerItem =
                                routerRecyclerAdapter.mDiffer
                                        .getCurrentList()
                                        .get(Math.toIntExact(entry.getKey()));
                        String messageId = String.valueOf(routerItem.getMessage_id());
                        RouterHandler.removeWorkForMessage(getApplicationContext(), messageId);
                        routerRecyclerAdapter.notifyItemChanged(Math.toIntExact(entry.getKey()));
                    }
                    return true;
                }
                return true;
            }
            return false;
        }

        // Called when the user exits the action mode.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            routerRecyclerAdapter.resetAllSelected();
        }
    };
}