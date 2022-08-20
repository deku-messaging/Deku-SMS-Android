package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.example.swob_server.Models.MessagesThreadRecyclerAdapter;
import com.example.swob_server.Models.SMS;
import com.example.swob_server.Models.SMSHandler;
import com.example.swob_server.Models.SingleMessagesThreadRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MessagesThreadsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_threads);

        populateMessageThreads();
    }

    List<SMS> getThreadsFromCursor(Cursor cursor) {
        List<SMS> threadsInCursor = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do{
                SMS sms = new SMS(cursor, true);
                threadsInCursor.add(sms);
                /*
                for(String colName : cursor.getColumnNames()) {
                    Log.i("", "col name: " + colName);
                }
                 */
            }
            while(cursor.moveToNext());
        }
        else {
            Log.i(this.getLocalClassName(), "No threads in cursor");
        }

        return threadsInCursor;
    }

    void populateMessageThreads() {
        String threadId = "2";
        String address = "+15555215554";
        Cursor cursor = SMSHandler.fetchSMSMessagesThreads(getApplicationContext(), threadId);
//        Cursor cursor = SMSHandler.fetchAllSMSMessages(getApplicationContext());
//        Cursor cursor = SMSHandler.fetchSMSMessagesAddress(getApplicationContext(), address);

        List<SMS> messagesForThread = getThreadsFromCursor(cursor);
        messagesForThread = SMSHandler.getAddressForThreads(getApplicationContext(), messagesForThread);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.messages_threads_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, messagesForThread, R.layout.messages_threads_layout);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        linearLayoutManager.setReverseLayout(false);
//        linearLayoutManager.setStackFromEnd(false);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }
}