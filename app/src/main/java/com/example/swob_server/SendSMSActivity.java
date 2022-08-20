package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.swob_server.Models.SingleMessagesThreadRecyclerAdapter;
import com.example.swob_server.Models.SMS;
import com.example.swob_server.Models.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class SendSMSActivity extends AppCompatActivity {

    List<String> messagesList = new ArrayList();

    public static final String ADDRESS = "address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);
        Log.d("", "Composing number: " + getIntent().getStringExtra(ADDRESS));
        populateMessageThread();
    }

    List<SMS> getMessagesFromCursor(Cursor cursor) {
        List<SMS> messagesInThread = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do{
                SMS sms = new SMS(cursor);
                messagesInThread.add(sms);
            }
            while(cursor.moveToNext());
        }
        else {
            Log.i(this.getLocalClassName(), "No messages to show");
        }

        return messagesInThread;
    }


    void populateMessageThread() {
        // Cursor cursor = SMSHandler.fetchSMSMessages(getApplicationContext(), "emulator-5554");
        String threadId = "2";
        // String address = "emulator-5554";
        // String address = "5555215554";
//        String address = "+15555215554";
        String address = getIntent().getStringExtra(ADDRESS);
//        Cursor cursor = SMSHandler.fetchSMSMessagesThreads(getApplicationContext(), threadId);
//        Cursor cursor = SMSHandler.fetchAllSMSMessages(getApplicationContext());
        Cursor cursor = SMSHandler.fetchSMSMessagesAddress(getApplicationContext(), address);

        List<SMS> messagesForThread = getMessagesFromCursor(cursor);
        for(SMS sms: messagesForThread) {
            Log.i("Message: ", "body: " + sms.getBody());
            Log.i("Thread ID: ", "thread_id: " + sms.getThreadId() );
            Log.i("Type: ", "type: " + sms.getType() );
        }

        RecyclerView singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);

        SingleMessagesThreadRecyclerAdapter singleMessagesThreadRecyclerAdapter = new SingleMessagesThreadRecyclerAdapter(
                this, messagesForThread, R.layout.messages_thread_received_layout, R.layout.messages_thread_sent_layout);

        singleMessagesThreadRecyclerView.setAdapter(singleMessagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        singleMessagesThreadRecyclerView.scrollToPosition(messagesForThread.size() - 1);
    }

    public void sendMessage(View view) {
        // TODO: Have interns for -
        // TODO: sending
        // TODO: delivered
        // TODO: failed
        String destinationAddress = getIntent().getStringExtra(ADDRESS);
        TextView smsTextView = findViewById(R.id.sms_text);
        String text = smsTextView.getText().toString();

        try {
            SMSHandler.sendSMS(getApplicationContext(), destinationAddress, text);
        }
        catch(IllegalAccessError e ) {
            e.printStackTrace();
        }
        finally {
            smsTextView.setText("");
        }
    }

}