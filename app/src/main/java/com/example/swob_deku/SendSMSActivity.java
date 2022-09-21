package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SingleMessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS;
import com.example.swob_deku.Models.SMSHandler;
import com.google.android.material.textfield.TextInputEditText;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class SendSMSActivity extends AppCompatActivity {

    List<String> messagesList = new ArrayList();

    public static final String ADDRESS = "address";
    public static final String THREAD_ID = "thread_id";
    public static final String BODY = "body";
    public static final String ID = "_id";

    public static final String SMS_SENT_INTENT = "SMS_SENT";
    public static final String SMS_DELIVERED_INTENT = "SMS_DELIVERED";

    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    boolean currentlyActive = false;

    SingleMessagesThreadRecyclerAdapter singleMessagesThreadRecyclerAdapter;
    RecyclerView singleMessagesThreadRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.send_smsactivity_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        currentlyActive = true;
        if(!checkPermissionToSendSMSMessages())
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);

        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceStates) {
        super.onPostCreate(savedInstanceStates);
        processForSharedIntent();
        handleIncomingMessage();
        cancelNotifications();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ActionBar ab = getSupportActionBar();
                String address = getIntent().getStringExtra(ADDRESS);

                // TODO: if has letters, make sure reply cannot happen
                ab.setTitle(Contacts.retrieveContactName(getApplicationContext(), address));
                populateMessageThread();
            }
        }).start();
    }

    private void processForSharedIntent() {
        String indentAction = getIntent().getAction();
        if(indentAction != null && getIntent().getAction().equals(Intent.ACTION_SENDTO)) {
            String sendToString = getIntent().getDataString();
            if(sendToString.contains("%2B"))
                sendToString = sendToString.replace("%2B", "");

            Log.d("", "Working on a shared Intent... " + sendToString);

            if(sendToString.indexOf("smsto:") > -1 || sendToString.indexOf("sms:") > -1) {
               String address = sendToString.substring(sendToString.indexOf(':') + 1);
               String text = getIntent().getStringExtra("sms_body");

               getIntent().putExtra(ADDRESS, address);
               TextInputEditText send_text = findViewById(R.id.sms_text);
               send_text.setText(text);
            }
        }
    }

    private void handleSentMessages() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)
        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(ID, -1);
                switch(getResultCode()) {

                    case Activity.RESULT_OK:
                        try {
                            SMSHandler.registerSentMessage(context, id);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    default:
                        SMSHandler.registerFailedMessage(context, id, getResultCode());
                }
                if(isCurrentlyActive()) {
                    updateStack();
                    unregisterReceiver(this);
                }
            }
        };
        registerReceiver(sentBroadcastReceiver, new IntentFilter(SMS_SENT_INTENT));
    }

    public boolean isCurrentlyActive() {
        return this.getWindow().getDecorView().getRootView().isShown();
    }

    private void handleDeliveredMessages() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)
        BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(ID, -1);
                switch(getResultCode()) {

                    case Activity.RESULT_OK:
                        SMSHandler.registerDeliveredMessage(context, id);
                        break;
                }
                if(isCurrentlyActive()) {
                    updateStack();
                    unregisterReceiver(this);
                }
            }
        };
        registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_INTENT));
    }

    public void cancelNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                getApplicationContext());

        if(getIntent().hasExtra(THREAD_ID))
            notificationManager.cancel(Integer.parseInt(getIntent().getStringExtra(THREAD_ID)));
    }

    private void handleIncomingMessage() {
        BroadcastReceiver incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                    for (SmsMessage currentSMS: Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        // currentSMS = SMSHandler.getIncomingMessage(aObject, bundle);

                        // TODO: Fetch address name from contact list if present
                        String address = currentSMS.getDisplayOriginatingAddress();
                        Cursor cursor = SMSHandler.fetchSMSMessagesAddress(context, address);
                        if(cursor.moveToFirst()) {
                            SMS sms = new SMS(cursor);
                            if (isCurrentlyActive() && sms.getThreadId().equals(getIntent().getStringExtra(THREAD_ID))) {
                                getIntent().putExtra(ADDRESS, sms.getAddress());
                                cancelNotifications();
                                updateStack();
                            }
                        }
                    }
                }
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
         registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

    List<SMS> getMessagesFromCursor(Cursor cursor) {
        List<SMS> appendedList = new ArrayList<>();
        Date previousDate = null;

        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();

        DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        if(cursor.moveToFirst()) {
            do {
                SMS sms = new SMS(cursor);
                Date date = new Date(Long.parseLong(sms.getDate()));
                if (previousDate != null) {
                    calendar1.setTime(date);
                    calendar2.setTime(previousDate);

                    if (calendar1.get(Calendar.DATE) < calendar2.get(Calendar.DATE)) {
                        String dateStr = dateFormat.format(previousDate);
                        appendedList.add(new SMS(dateStr));
                    }
                }

                appendedList.add(sms);
                previousDate = date;
            }
            while (cursor.moveToNext());

            String dateStr = dateFormat.format(previousDate);
            appendedList.add(new SMS(dateStr));
        }

        return appendedList;
    }


    void populateMessageThread() {
        String threadId = "-1";
        if(getIntent().hasExtra(THREAD_ID))
            threadId = getIntent().getStringExtra(THREAD_ID);

        else if(getIntent().hasExtra(ADDRESS)) {
            Cursor cursor = SMSHandler.fetchSMSMessagesAddress(getApplicationContext(), getIntent().getStringExtra(ADDRESS));
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                threadId = sms.getThreadId();
            }
        }

        Cursor cursor = SMSHandler.fetchSMSMessagesThread(getApplicationContext(), threadId);
        List<SMS> messagesForThread = getMessagesFromCursor(cursor);

        /*
        if(messagesForThread.size() > 0 && !messagesForThread.get(0).replyPathPreset) {
            ConstraintLayout sendlayout = findViewById(R.id.send_message_content_layouts);
            sendlayout.setVisibility(View.GONE);
        }

         */

        singleMessagesThreadRecyclerAdapter = new SingleMessagesThreadRecyclerAdapter(
                this,
                messagesForThread,
                R.layout.messages_thread_received_layout,
                R.layout.messages_thread_sent_layout,
                R.layout.messages_thread_timestamp_layout);

        singleMessagesThreadRecyclerView.setAdapter(singleMessagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(false);
        linearLayoutManager.setReverseLayout(true);

        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
//        singleMessagesThreadRecyclerView.scrollToPosition(messagesForThread.size() - 1);

        String finalThreadId = threadId;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SMSHandler.updateSMSMessagesThreadStatus(getApplicationContext(), finalThreadId, "1");
                }
                catch(Exception e ) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }


    public void sendMessage(View view) {
        // TODO: Don't let sending happen if message box is empty
        String destinationAddress = getIntent().getStringExtra(ADDRESS);
        TextView smsTextView = findViewById(R.id.sms_text);
        String text = smsTextView.getText().toString();

        try {
//            SMSHandler.registerOutgoingMessage(getApplicationContext(), destinationAddress, text);
            long messageId = Helpers.generateRandomNumber();
            Intent sentIntent = new Intent(SMS_SENT_INTENT);
            sentIntent.putExtra(ID, messageId);

            Intent deliveredIntent = new Intent(SMS_DELIVERED_INTENT);
            deliveredIntent.putExtra(ID, messageId);

             PendingIntent sentPendingIntent = PendingIntent.getBroadcast(this, 200,
                     sentIntent,
                     PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(this, 150,
                    deliveredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            handleSentMessages();
            handleDeliveredMessages();

            SMSHandler.sendSMS(getApplicationContext(), destinationAddress, text,
                    sentPendingIntent, deliveredPendingIntent, messageId);

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

    @Override
    protected void onResume() {
        super.onResume();

        updateStack();
        cancelNotifications();
    }

}