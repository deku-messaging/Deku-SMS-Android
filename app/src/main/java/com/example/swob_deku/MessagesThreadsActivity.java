package com.example.swob_deku;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.example.swob_deku.Models.SMS.PDUConverter;
import com.example.swob_deku.Models.SMS.SMS;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MessagesThreadsActivity extends AppCompatActivity {
    // TODO: Change address to friendly name if in phonebook
    MessagesThreadViewModel messagesThreadViewModel;
    MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_threads);

//        cancelAllNotifications();
        handleIncomingMessage();

        messagesThreadViewModel = new ViewModelProvider(this).get(
                MessagesThreadViewModel.class);

        TextInputEditText searchTextView = findViewById(R.id.recent_search_edittext_clickable);
        searchTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    startActivity(new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class));
                }
            }
        });
        searchTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class));
            }
        });

        TextInputLayout searchTextViewLayout = findViewById(R.id.search_messages_text_clickable);
        searchTextViewLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.messages_threads_menu_item_settings: {
                                Intent settingsIntent = new Intent(getApplicationContext(),
                                        GatewayServerListingActivity.class);
                                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(settingsIntent);
                                return true;
                            }
                            default:
                                return false;
                        }
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.main_menu, popup.getMenu());
                popup.show();
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.messages_threads_recycler_view);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        messagesThreadViewModel.getMessages(getApplicationContext()).observe(this,
                new Observer<List<SMS>>() {
                    @Override
                    public void onChanged(List<SMS> smsList) {
                        messagesThreadRecyclerAdapter.submitList(smsList);
                    }
                });


        // TODO: remove
        new Thread(new Runnable() {
            @Override
            public void run() {
//                int[] receivedPDU = {0x07,0x91,0x32,0x67,0x49,0x00,0x00,0x71,0x24,0x0c,0x91,0x32,0x67,0x09,
//                        0x28,0x26,0x24,0x00,0x00,0x32,0x20,0x91,0x01,0x73,0x74,0x40,0x07,0xe8,0x72,
//                        0x1e,0xd4,0x2e,0xbb,0x01};

//                String DA = "+237652156811";
//                String encodedPhoneNumber = PDUConverter.encodePhoneNumber(DA);
//                Log.d(getLocalClassName(), "PDU encoded phonenumber: " + encodedPhoneNumber);

                /*
0011000c91326709282624 00 00 322012208360040BE 8329BFD6DDDF723619
0011000C91326709282624 00 00 0B0BE 8329BFD06DDDF723619
0011000C91326709282624 00 00 FF0BE 8329BFD06DDDF723619
                 */

//                PDUConverter.PDUEncoded pduEncoded = PDUConverter.encode("", "+237690826242", "", "hello world");
                PDUConverter.PDUEncoded pduEncoded = PDUConverter.encode("", "+237690826242", "", "hello world now");
                String encoded = pduEncoded.getPduEncoded();
                Log.d(getLocalClassName(), "PDU encoded: " + encoded);

//                encoded = encoded.replaceFirst("01", "00");
//                Log.d(getLocalClassName(), "PDU encoded: " + encoded);
//                ByteArrayOutputStream sendingPDU = new ByteArrayOutputStream();
//                try {
//                    sendingPDU.write(new byte[]{0x00, 0x11, 0x00, 0x0B});
//                    SmsMessage smsMessage = SmsMessage.createFromPdu(sendingPDU.toByteArray());
////                    sendingPDU.write(encodedPhoneNumber.getBytes(StandardCharsets.UTF_8));
//                    Log.d(getLocalClassName(), "PDU: " + Integer.parseInt(encodedPhoneNumber));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }

//                try {
//                    SMSHandler.interpret_PDU(DataHelper.intArrayToByteArray(pdu));
//                } catch (ParseException e) {
//                    throw new RuntimeException(e);
//                }
            }
        }).start();
    }

    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancelAll();
    }

    public void onNewMessageClick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    public void onRouterClick(View view) {
        startActivity(new Intent(this, RouterActivity.class));
    }

    private void handleIncomingMessage() {
        BroadcastReceiver incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                messagesThreadViewModel.informChanges(getApplicationContext());
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }


    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (1) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor contactCursor = getApplicationContext().getContentResolver().query(
                            contactData,
                            null,
                            null,
                            null,
                            null);

                    if(contactCursor != null) {
                        if (contactCursor.moveToFirst()) {
                            int contactIndexInformation = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String number = contactCursor.getString(contactIndexInformation);

                            Intent singleMessageThreadIntent = new Intent(this, SMSSendActivity.class);
                            singleMessageThreadIntent.putExtra(SMSSendActivity.ADDRESS, number);
                            startActivity(singleMessageThreadIntent);
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.messages_threads_recycler_view).requestFocus();
        messagesThreadViewModel.informChanges(getApplicationContext());
    }

}