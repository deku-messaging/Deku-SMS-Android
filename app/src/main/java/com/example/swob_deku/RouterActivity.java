package com.example.swob_deku;

import static android.view.View.GONE;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.CustomAppCompactActivity;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.SingleMessageViewModel;
import com.example.swob_deku.Models.Messages.ViewHolders.TemplateViewHolder;
import com.example.swob_deku.Models.Router.Router;
import com.example.swob_deku.Models.Router.RouterHandler;
import com.example.swob_deku.Models.Router.RouterViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.Route;

public class RouterActivity extends CustomAppCompactActivity {

    RouterViewModel routerViewModel;
    public MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;
    RecyclerView routedMessageRecyclerView;

    ActionBar ab;

    Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        myToolbar = (Toolbar) findViewById(R.id.routed_messages_toolbar);
        myToolbar.setTitle(R.string.homepage_menu_routed);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager);

        messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter( this,
                true, "", this);

        routedMessageRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        routerViewModel = new ViewModelProvider(this).get(
                RouterViewModel.class);

        routerViewModel.getMessages(getApplicationContext()).observe(this,
                new Observer<List<SMS>>() {
                    @Override
                    public void onChanged(List<SMS> smsList) {
                        messagesThreadRecyclerAdapter.submitList(smsList);
                        if(!smsList.isEmpty())
                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.GONE);
                        else {
                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.VISIBLE);
                            routedMessageRecyclerView.smoothScrollToPosition(0);
                        }
                    }
                });

        listeners();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        configureBroadcastListeners(new Runnable() {
            @Override
            public void run() {
                Log.d(getLocalClassName(), "Updating the routing information");
                routerViewModel.informChanges(getApplicationContext());
            }
        });

    }

    private void listeners() {
        messagesThreadRecyclerAdapter.selectedItems.observe(this, new Observer<HashMap<String, TemplateViewHolder>>() {
            @Override
            public void onChanged(HashMap<String, TemplateViewHolder> stringTemplateViewHolderHashMap) {
                if(stringTemplateViewHolderHashMap != null) {
                    myToolbar.getMenu().findItem(R.id.messages_thread_routing_delete)
                            .setVisible(!stringTemplateViewHolderHashMap.isEmpty());
                }
                else
                    myToolbar.getMenu().findItem(R.id.messages_thread_routing_delete)
                            .setVisible(false);
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
        if(item.getItemId() == R.id.messages_thread_routing_delete) {
            if(messagesThreadRecyclerAdapter.selectedItems.getValue() != null) {
                for (Map.Entry<String, TemplateViewHolder> entry :
                        messagesThreadRecyclerAdapter.selectedItems.getValue().entrySet()) {
                    String messageId = String.valueOf(entry.getValue().messageId);
                    Log.d(getLocalClassName(), "Removing routing message: " + messageId);
                    RouterHandler.removeWorkForMessage(getApplicationContext(), messageId);
                }
                messagesThreadRecyclerAdapter.resetAllSelectedItems();
                routerViewModel.informChanges(getApplicationContext());
                return true;
            }
        }
        return false;
    }
}