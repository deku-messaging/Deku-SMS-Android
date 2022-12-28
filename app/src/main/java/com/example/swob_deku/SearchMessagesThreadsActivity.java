package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchMessagesThreadsActivity extends AppCompatActivity {

    // TODO: custom search with startIcon being up button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_messages_threads);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.search_messages_toolbar);
        myToolbar.setTitle(R.string.search_messages_text);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);


        // TextInputLayout textInputLayout = findViewById(R.id.search_messages_text);
        TextInputEditText searchTextInput = findViewById(R.id.new_gateway_client_url_input);
        searchTextInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView searchView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String searchInput = searchView.getText().toString();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    populateMessageThreads(searchInput);

                    findViewById(R.id.search_results_recycler_view).requestFocus();

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

//        messagesForThread = SMSHandler.getAddressForThreads(getApplicationContext(), messagesForThread, false);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.search_results_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout, true, searchInput);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }
}