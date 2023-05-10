package com.example.swob_deku;

import static android.view.View.GONE;

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
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.SingleMessageViewModel;
import com.example.swob_deku.Models.Router.Router;
import com.example.swob_deku.Models.Router.RouterViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RouterActivity extends AppCompatActivity {

    RouterViewModel routerViewModel;
    public MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;
    RecyclerView routedMessageRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.routed_messages_toolbar);
        myToolbar.setTitle(R.string.router_name);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager);

        // TODO: search - and goto message in adapter
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

        handleIncomingMessage();
    }

    private void handleIncomingMessage() {
        BroadcastReceiver incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                routerViewModel.informChanges(getApplicationContext());
//                routedMessageRecyclerView.smoothScrollToPosition(0);
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

}