package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.swob_server.Models.MessagesThreadRecyclerAdapter;
import com.example.swob_server.Models.SMS;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SendSMSActivity extends AppCompatActivity {

    List<String> messagesList = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);

        // Cursor cursor = SMSHandler.fetchSMSMessages(getApplicationContext(), "emulator-5554");
        String threadId = "2";
//        Cursor cursor = SMSHandler.fetchSMSMessagesThreads(getApplicationContext(), threadId);
        Cursor cursor = SMSHandler.fetchAllSMSMessages(getApplicationContext());

        List<SMS> messagesForThread = getMessagesForThread(cursor);
        populateMessageThread(messagesForThread);

    }

    List<SMS> getMessagesForThread(Cursor cursor) {
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


    void populateMessageThread(List<SMS> messagesForThread) {
        for(SMS sms: messagesForThread) {
            Log.i("Message: ", "body: " + sms.getBody());
            Log.i("Type: ", "type: " + sms.getType() + "\n");
        }

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.messages_thread_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, messagesForThread, R.layout.messages_thread_received_layout, R.layout.messages_thread_sent_layout);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(false);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }

    public void sendMessage(View view) {
        // TODO: Have interns for -
        // TODO: sending
        // TODO: delivered
        // TODO: failed
        String destinationAddress = "5555215554";
        TextView smsTextView = findViewById(R.id.sms_text);
        String text = smsTextView.getText().toString();

        try {
            SMSHandler.sendSMS(getApplicationContext(), destinationAddress, text);
        }
        catch(IllegalAccessError e ) {
            e.printStackTrace();
        }
    }

}