package com.example.swob_server;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class SMSReceiverActivity extends BroadcastReceiver {
    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if(intent.hasExtra("ADDRESS"))
            Toast.makeText(context, "Got a broadcast, wonder what it's all about", Toast.LENGTH_LONG).show();

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
//                    Toast.makeText(context, "Message Delivered!", Toast.LENGTH_LONG).show();
//
//                    Log.d("", "Intents pdu: " + intent.getStringExtra("pdus"));
//                    Log.d("", "Intents pdu format: " + intent.getStringExtra("format"));
//                    Log.d("", "Intents pdu phone: " + intent.getStringExtra("phone"));
//                    Log.d("", "Intents pdu errorcode: " + intent.getStringExtra("errorCode"));
//                    SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
//                    for (SmsMessage smsMessage : smsMessages) {
//                        Log.d("", "pdu message: " + smsMessage.getMessageBody());
//                        Log.d("", "pdu status: " + smsMessage.getStatus());
//                        Log.d("", "pdu originating address: " + smsMessage.getOriginatingAddress());
//                        Log.d("", "pdu status report message: " + smsMessage.isStatusReportMessage());
//                    }
//                    break;
            }
        }
        else if (intent.getAction().equals(SendSMSActivity.SMS_SENT_INTENT)) {
        }
    }
}