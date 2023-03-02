package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.SingleMessageViewModel;
import com.example.swob_deku.Models.Messages.SingleMessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class SMSSendActivity extends AppCompatActivity {

    // TODO: incoming message MessagesThread
    // TODO: incoming message from notification
    // TODO: incoming message from shared intent

    SingleMessagesThreadRecyclerAdapter singleMessagesThreadRecyclerAdapter;
    SingleMessageViewModel singleMessageViewModel;
    TextInputEditText smsTextView;
    TextInputLayout smsTextInputLayout;

    MutableLiveData<String> mutableLiveDataComposeMessage = new MutableLiveData<>();

    public static final String COMPRESSED_IMAGE_BYTES = "COMPRESSED_IMAGE_BYTES";
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String ADDRESS = "address";
    public static final String THREAD_ID = "thread_id";
    public static final String ID = "_id";
    public static final String SEARCH_STRING = "search_string";

    public static final String SMS_SENT_INTENT = "SMS_SENT";
    public static final String SMS_DELIVERED_INTENT = "SMS_DELIVERED";

    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    private final int RESULT_GALLERY = 100;

    String threadId = "";
    String address = "";

    String contactName = "";

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);
        toolbar = (Toolbar) findViewById(R.id.send_smsactivity_toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        smsTextView = findViewById(R.id.sms_text);

        handleIncomingBroadcast();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    determineAddress();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                improveMessagingUX();

                contactName = Contacts.retrieveContactName(getApplicationContext(), address);
                contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                        address: contactName;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ab.setTitle(contactName);
                        } catch(Exception e ) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();

        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                findViewById(R.id.sms_send_button).setVisibility(s.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    private void determineAddress() throws InterruptedException {
        processForSharedIntent();
        checkSendingImage();
        getMessagesThreadId();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(false);
        linearLayoutManager.setReverseLayout(true);

        RecyclerView singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);

        Long focusId = getIntent().hasExtra(ID) ? Long.parseLong(getIntent().getStringExtra(ID)) : null;
        String searchString = getIntent().hasExtra(SEARCH_STRING) ? getIntent().getStringExtra(SEARCH_STRING) : null;

        singleMessagesThreadRecyclerAdapter = new SingleMessagesThreadRecyclerAdapter(
                this,
                R.layout.messages_thread_received_layout,
                R.layout.messages_thread_sent_layout,
                R.layout.messages_thread_timestamp_layout,
                focusId,
                searchString,
                singleMessagesThreadRecyclerView, toolbar);

        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        singleMessagesThreadRecyclerView.setAdapter(singleMessagesThreadRecyclerAdapter);

        singleMessageViewModel = new ViewModelProvider(this).get(
                SingleMessageViewModel.class);

        singleMessageViewModel.getMessages(getApplicationContext(), threadId).observe(this,
                new Observer<List<SMS>>() {
                    @Override
                    public void onChanged(List<SMS> smsList) {
                        singleMessagesThreadRecyclerAdapter.submitList(smsList);
                    }
                });


        if(mutableLiveDataComposeMessage.getValue() == null || mutableLiveDataComposeMessage.getValue().isEmpty())
            findViewById(R.id.sms_send_button).setVisibility(View.INVISIBLE);
        updateMessagesToRead();
    }

    private void checkSendingImage() throws InterruptedException {
        if(getIntent().hasExtra(THREAD_ID) || (this.threadId != null && !this.threadId.isEmpty())) {
            this.threadId = getIntent().getStringExtra(THREAD_ID);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    /**
//                     * TODO: check if message in OUTBOX status PENDING
//                     * TODO: send message to smshandler to handle sending them out.
//                     */
//
//                    Cursor cursor = SMSHandler.fetchSMSOutboxPendingForThread(getApplicationContext(), threadId);
//                    Log.d(getLocalClassName(), "SMS Outbox found: " + cursor.getCount());
//
//                    if(cursor.moveToFirst()) {
//                        do {
//                            SMS sms = new SMS(cursor);
//                            /**
//                             * TODO: check if can base64 and bitmap - then data
//                             * TODO: else text
//                             */
//                            try {
//                                byte[] data = Base64.decode(sms.getBody(), Base64.NO_PADDING);
//                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//                                if(bitmap != null) {
//                                    SMSHandler.sendDataSMS(getApplicationContext(), sms.getAddress(), data,
//                                            null, null, Long.parseLong(sms.getId()));
//                                    continue;
//                                }
//                            } catch(Exception e) {
//                                e.printStackTrace();
//                            }
//
//                            SMSHandler.sendTextSMS(getApplicationContext(), sms.getAddress(), sms.getBody(),
//                                    null, null, Long.parseLong(sms.getId()));
//                        } while(cursor.moveToNext());
//                    }
//                    cursor.close();
//                }
//            }).start();
//
        }

        if(getIntent().hasExtra(ADDRESS))
            this.address = getIntent().getStringExtra(ADDRESS);
    }

    private void updateMessagesToRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Updating read for threadID: " + threadId);
               SMSHandler.updateThreadMessagesThread(getApplicationContext(), threadId);
            }
        }).start();
    }

    private void getMessagesThreadId() {
        if(getIntent().hasExtra(THREAD_ID)) {
            threadId = getIntent().getStringExtra(THREAD_ID);
            Cursor cursor = SMSHandler.fetchSMSAddressFromThreadId(getApplicationContext(), threadId);

            if(cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
                address = String.valueOf(cursor.getString(addressIndex));

                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Found Address: " + address);
            }

            cursor.close();
        }

        else if(getIntent().hasExtra(ADDRESS) || !address.isEmpty()) {
            if(address.isEmpty())
                address = getIntent().getStringExtra(ADDRESS);

            if(BuildConfig.DEBUG)
                Log.d(getLocalClassName(), "Searching thread ID with address: " + address);

            Cursor cursor = SMSHandler.fetchSMSThreadIdFromAddress(getApplicationContext(), address);
            if(cursor.moveToFirst()) {
                int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
                threadId = String.valueOf(cursor.getString(threadIdIndex));

                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Found thread ID: " + threadId);
            }
            cursor.close();
        }
    }

    private void improveMessagingUX() {

        smsTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                view.getParent().requestDisallowInterceptTouchEvent(true);
                if ((motionEvent.getAction() & MotionEvent.ACTION_UP) != 0 &&
                        (motionEvent.getActionMasked() & MotionEvent.ACTION_UP) != 0)
                {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        });


        smsTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mutableLiveDataComposeMessage.setValue(s.toString());
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!PhoneNumberUtils.isWellFormedSmsAddress(address)) {
                        ConstraintLayout smsLayout = findViewById(R.id.send_message_content_layouts);
                        smsLayout.setVisibility(View.GONE);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processForSharedIntent() {
        String indentAction = getIntent().getAction();

        if(indentAction != null && getIntent().getAction().equals(Intent.ACTION_SENDTO)) {
            String sendToString = getIntent().getDataString();

            if(BuildConfig.DEBUG)
                Log.d("", "Processing shared #: " + sendToString);

            sendToString = sendToString.replace("%2B", "+")
                            .replace("%20", "");

            if(sendToString.indexOf("smsto:") > -1 || sendToString.indexOf("sms:") > -1) {
               address = sendToString.substring(sendToString.indexOf(':') + 1);
               String text = getIntent().getStringExtra("sms_body");

               // TODO: should inform view about data being available
               if(getIntent().hasExtra(Intent.EXTRA_INTENT)) {
                   byte[] bytesData = getIntent().getByteArrayExtra(Intent.EXTRA_STREAM);
                   if (bytesData != null) {
                       Log.d(getClass().getName(), "Byte data: " + bytesData);
                       Log.d(getClass().getName(), "Byte data: " + new String(bytesData, StandardCharsets.UTF_8));

                       text = new String(bytesData, StandardCharsets.UTF_8);
                       getIntent().putExtra(Intent.EXTRA_INTENT, getIntent().getByteArrayExtra(Intent.EXTRA_INTENT));
                   }
               }

               if(text != null && !text.isEmpty()) {
                   smsTextView.setText(text);
                   String finalText = text;
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           mutableLiveDataComposeMessage.setValue(finalText);
                       }
                   });
               }
            }
        }
    }

    public void handleIncomingBroadcast() {
        BroadcastReceiver incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                singleMessageViewModel.informChanges(context);
                cancelNotifications(getIntent().getStringExtra(THREAD_ID));
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

    public void handleBroadcast() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)

        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                long id = intent.getLongExtra(ID, -1);
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Broadcast received for sent: " + id);
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        try {
                            SMSHandler.registerSentMessage(getApplicationContext(), id);
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
                        try {
                            SMSHandler.registerFailedMessage(context, id, getResultCode());
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        if(BuildConfig.DEBUG) {
                            Log.d(getLocalClassName(), "Failed to send: " + getResultCode());
                            Log.d(getLocalClassName(), "Failed to send: " + getResultData());
                            Log.d(getLocalClassName(), "Failed to send: " + intent.getData());
                        }
                }

                singleMessageViewModel.informChanges(getApplicationContext(), id);
                unregisterReceiver(this);
            }
        };

        BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(ID, -1);

                if (getResultCode() == Activity.RESULT_OK) {
                    SMSHandler.registerDeliveredMessage(context, id);
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(getLocalClassName(), "Failed to deliver: " + getResultCode());
                }

                singleMessageViewModel.informChanges(getApplicationContext(), id);
                unregisterReceiver(this);
            }
        };

        registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_INTENT));
        registerReceiver(sentBroadcastReceiver, new IntentFilter(SMS_SENT_INTENT));

    }

    public void cancelNotifications(String threadId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                getApplicationContext());

        if(getIntent().hasExtra(THREAD_ID))
            notificationManager.cancel(Integer.parseInt(threadId));
    }

    public PendingIntent[] getPendingIntents(long messageId) {
        Intent sentIntent = new Intent(SMS_SENT_INTENT);
        sentIntent.putExtra(SMSSendActivity.ID, messageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_INTENT);
        deliveredIntent.putExtra(SMSSendActivity.ID, messageId);

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                Integer.parseInt(String.valueOf(messageId)), sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                Integer.parseInt(String.valueOf(messageId)), deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
    }

    public void sendTextMessage(View view) {
        // TODO: Don't let sending happen if message box is empty
        String text = smsTextView.getText().toString();

        try {
            long messageId = Helpers.generateRandomNumber();
            PendingIntent[] pendingIntents = getPendingIntents(messageId);

            handleBroadcast();

            String tmpThreadId = SMSHandler.sendTextSMS(getApplicationContext(), address, text,
                    pendingIntents[0], pendingIntents[1], messageId);

            resetSmsTextView();
            if(!tmpThreadId.equals("null") && !tmpThreadId.isEmpty()) {
                threadId = tmpThreadId;
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Refreshing with threadId: " + threadId);
                singleMessageViewModel.informChanges(getApplicationContext(), threadId);
            }
            else {
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Refreshing with messageId: " + messageId);
                singleMessageViewModel.informChanges(getApplicationContext(), messageId);
            }
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

    private void resetSmsTextView() {
//        smsTextView.setText(null);
        smsTextView.getText().clear();
    }

    public boolean checkPermissionToSendSMSMessages() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    public void onBackPressed() {
        startActivity(new Intent(this, MessagesThreadsActivity.class));
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void uploadImage(View view) {
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent , RESULT_GALLERY );
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_GALLERY) {
            if (null != data) {
                Uri imageUri = data.getData();

                Intent intent = new Intent(this, ImageViewActivity.class);
                intent.putExtra(IMAGE_URI, imageUri.toString());
                intent.putExtra(ADDRESS, address);
                startActivity(intent);
                finish();
            }
        }
    }
}