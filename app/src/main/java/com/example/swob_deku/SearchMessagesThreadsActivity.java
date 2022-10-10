package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.example.swob_deku.Models.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS;
import com.example.swob_deku.Models.SMSHandler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class SearchMessagesThreadsActivity extends AppCompatActivity {

    // TODO: custom search with startIcon being up button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_messages_threads);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.search_messages_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);


        // TextInputLayout textInputLayout = findViewById(R.id.search_messages_text);
        TextInputEditText searchTextInput = findViewById(R.id.recent_search_edittext);
        searchTextInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView searchView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String searchInput = searchView.getText().toString();
                    Log.d("", "initiating searching: " + searchInput);
                    populateMessageThreads(searchInput);

                    return true;
                }
                return false;
            }
        });
    }

    List<SMS> getThreadsFromCursor(Cursor cursor) {
        List<SMS> threadsInCursor = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do{
                SMS sms = new SMS(cursor);
                threadsInCursor.add(sms);
            }
            while(cursor.moveToNext());
        }
        else {
            Log.i(this.getLocalClassName(), "No threads in cursor");
        }

        return threadsInCursor;
    }

    void populateMessageThreads(String searchInput) {
        Cursor cursor = SMSHandler.fetchSMSMessagesForSearch(getApplicationContext(), searchInput);

        List<SMS> messagesForThread = getThreadsFromCursor(cursor);

        Log.d("", "Found search: " + messagesForThread.size());

        messagesForThread = SMSHandler.getAddressForThreads(getApplicationContext(), messagesForThread);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.search_results_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, messagesForThread, R.layout.messages_threads_layout);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }
}