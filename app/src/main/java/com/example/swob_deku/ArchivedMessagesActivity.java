package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.example.swob_deku.Models.Archive.ArchivedViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Router.RouterViewModel;
import com.example.swob_deku.Models.SMS.SMS;

import java.util.List;

public class ArchivedMessagesActivity extends AppCompatActivity {

    public MessagesThreadRecyclerAdapter archivedThreadRecyclerAdapter;

    ArchivedViewModel archivedViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_messages);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.messages_archived_toolbar);
        myToolbar.setTitle(R.string.archived_messages_toolbar_title);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);


        RecyclerView archivedMessagesRecyclerView = findViewById(R.id.messages_archived_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        archivedMessagesRecyclerView.setLayoutManager(linearLayoutManager);

        // TODO: search - and goto message in adapter
        archivedThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout, true, "", this);

        archivedMessagesRecyclerView.setAdapter(archivedThreadRecyclerAdapter);

        archivedViewModel = new ViewModelProvider(this).get(
                ArchivedViewModel.class);

        try {
            archivedViewModel.getMessages(getApplicationContext()).observe(this,
                    new Observer<List<SMS>>() {
                        @Override
                        public void onChanged(List<SMS> smsList) {
                            archivedThreadRecyclerAdapter.submitList(smsList);
                            if(!smsList.isEmpty())
                                findViewById(R.id.messages_archived_no_messages).setVisibility(View.GONE);
                            else {
                                findViewById(R.id.messages_archived_no_messages).setVisibility(View.VISIBLE);
                                archivedMessagesRecyclerView.smoothScrollToPosition(0);
                            }
                        }
                    });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}