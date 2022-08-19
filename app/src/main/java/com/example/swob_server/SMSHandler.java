package com.example.swob_server;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.List;

public class SMSHandler {

    public static void sendSMS(Context context,  String destinationAddress, String text) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
            context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        try {
            smsManager.sendTextMessage(destinationAddress, null, text, null, null);
        }
        catch(IllegalAccessError e) {
            throw e;
        }
    }

    public static Cursor fetchSMSMessages(Context context, String destinationAddress) {
        String[] phoneNumber = new String[] { destinationAddress }; //the wanted phone number

        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "address=?",
                phoneNumber,
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesThreads(Context context, String threadId) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "thread_id=?",
                new String[] { threadId },
                null);

        return smsMessagesCursor;
    }


    public static Cursor fetchAllSMSMessages(Context context) {
        /*
            MESSAGE_TYPE_ALL    = 0;
            MESSAGE_TYPE_INBOX  = 1;
            MESSAGE_TYPE_SENT   = 2;
            MESSAGE_TYPE_DRAFT  = 3;
            MESSAGE_TYPE_OUTBOX = 4;
            MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
            MESSAGE_TYPE_QUEUED = 6; // for messages to send later
        */

        // Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                null,
                null,
                "date DESC");

        return cursor;

    }
}
