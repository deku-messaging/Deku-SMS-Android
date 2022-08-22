package com.example.swob_server;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SMSReceiverActivity extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.provider.Telephony.SMS_DELIVER")) {
            switch(getResultCode() ) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "Message Delivered!", Toast.LENGTH_LONG).show();

                    Log.d("", "Intents pdu: " + intent.getStringExtra("pdus"));
                    Log.d("", "Intents pdu format: " + intent.getStringExtra("format"));
                    Log.d("", "Intents pdu phone: " + intent.getStringExtra("phone"));
                    Log.d("", "Intents pdu errorcode: " + intent.getStringExtra("errorCode"));
                    SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    for (SmsMessage smsMessage : smsMessages) {
                        Log.d("", "pdu message: " + smsMessage.getMessageBody());
                        Log.d("", "pdu status: " + smsMessage.getStatus());
                        Log.d("", "pdu originating address: " + smsMessage.getOriginatingAddress());
                        Log.d("", "pdu status report message: " + smsMessage.isStatusReportMessage());
                    }
                    break;
            }
        }
    }
}