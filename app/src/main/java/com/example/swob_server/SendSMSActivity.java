package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swob_server.Models.SingleMessagesThreadRecyclerAdapter;
import com.example.swob_server.Models.SMS;
import com.example.swob_server.Models.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class SendSMSActivity extends AppCompatActivity {

    List<String> messagesList = new ArrayList();

    public static final String ADDRESS = "address";
    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    boolean currentlyActive = false;

    SingleMessagesThreadRecyclerAdapter singleMessagesThreadRecyclerAdapter;
    RecyclerView singleMessagesThreadRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);

        currentlyActive = true;
        if(!checkPermissionToSendSMSMessages())
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);

        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);

        handleIncomingMessage();
        populateMessageThread();
    }

    private void handleIncomingMessage() {
        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    for (SmsMessage currentSMS: Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        // currentSMS = SMSHandler.getIncomingMessage(aObject, bundle);

                        // TODO: Fetch address name from contact list if present
                        String address = currentSMS.getDisplayOriginatingAddress();
                        if (currentlyActive && address.equals(getIntent().getStringExtra(ADDRESS))) {
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                                    getApplicationContext());

                            notificationManager.cancel(8888);

                            updateStack();
                        }
                    }
                }
            }
        };
        registerReceiver(sentBroadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
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
        String address = getIntent().getStringExtra(ADDRESS);
        Cursor cursor = SMSHandler.fetchSMSMessagesAddress(getApplicationContext(), address);

        List<SMS> messagesForThread = getMessagesFromCursor(cursor);

        singleMessagesThreadRecyclerAdapter = new SingleMessagesThreadRecyclerAdapter(
                this,
                messagesForThread,
                R.layout.messages_thread_received_layout,
                R.layout.messages_thread_sent_layout);

        singleMessagesThreadRecyclerView.setAdapter(singleMessagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        singleMessagesThreadRecyclerView.scrollToPosition(messagesForThread.size() - 1);
    }

    public void updateMesageStatus() {
        TextView messageStatusTextView = findViewById(R.id.message_thread_sent_status_text);
        messageStatusTextView.setText("sending...");
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
            // SMSHandler.registerOutgoingMessage(getApplicationContext(), destinationAddress, text);
            SMSHandler.sendSMS(getApplicationContext(), destinationAddress, text, null, null);
            smsTextView.setText("");
            updateStack();
        }

        catch(IllegalArgumentException e ) {
            e.printStackTrace();
            Toast.makeText(this, "Make sure Address and Text are provided.", Toast.LENGTH_LONG).show();
        }
        catch(Exception e ) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong, check log stack", Toast.LENGTH_LONG).show();
        }

    }

    private void updateStack() {
       populateMessageThread();
    }

    public boolean checkPermissionToSendSMSMessages() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SEND_SMS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    Toast.makeText(this, "Let's do this!!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}