package com.example.swob_server.Models;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.List;

public class SMSHandler {

    public static void sendSMS(Context context, String destinationAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
            context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        try {
            smsManager.sendTextMessage(destinationAddress, null, text, sentIntent, deliveryIntent);
        }
        catch(Throwable e) {
            // throw new IllegalArgumentException(e);
            throw e;
        }
    }

    public static Cursor fetchSMSMessagesAddress(Context context, String address) {
        address = address.replaceAll("[\\s-]", "");
        Log.d("", "Composing to: " + address);

        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "address=?",
                new String[] { address },
                "date ASC");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesThread(Context context, String threadId) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                // new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                null,
                "thread_id=?",
                new String[] { threadId },
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesThreads(Context context) {
        String targetedURI = String.valueOf(Telephony.Sms.Conversations.CONTENT_URI);
        Cursor cursor = context.getContentResolver().query(
                Uri.parse(targetedURI),
                // new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                null,
                null,
                null,
                "date DESC");

        return cursor;
    }

    public static List<SMS> getAddressForThreads(Context context, List<SMS> messagesList) {
        for(int i=0; i< messagesList.size(); ++i) {
            String threadId = messagesList.get(i).getThreadId();
            Log.d("", "searching threadID: " + threadId);
            Cursor cursor = fetchSMSMessagesThread(context, threadId);

            if(cursor.moveToFirst()) {
                // assuming all the messages have the same address, just take the first one
                SMS sms = new SMS(cursor);
                messagesList.get(i).setAddress(sms.getAddress());
            }
        }
        return messagesList;
    }


    public static void registerIncomingMessage(Context context, SmsMessage smsMessage) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", smsMessage.getOriginatingAddress());
        contentValues.put("body", smsMessage.getMessageBody());
        context.getContentResolver().insert(Uri.parse(Telephony.Sms.Inbox.CONTENT_URI.toString()), contentValues);
    }

    public static void registerOutgoingMessage(Context context, String destinationAddress, String text) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", destinationAddress);
        contentValues.put("body", text);
        context.getContentResolver().insert(Uri.parse(Telephony.Sms.Outbox.CONTENT_URI.toString()), contentValues);
    }

    public static void registerFailedMessage(Context context, String destinationAddress, String text, int errorCode) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", destinationAddress);
        contentValues.put("body", text);
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_FAILED);
        contentValues.put("error_code", errorCode);
        context.getContentResolver().insert(Uri.parse(Telephony.Sms.Outbox.CONTENT_URI.toString()), contentValues);
    }

    public static void registerSentMessage(Context context, String destinationAddress, String text) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", destinationAddress);
        contentValues.put("body", text);
        context.getContentResolver().insert(Uri.parse(Telephony.Sms.Sent.CONTENT_URI.toString()), contentValues);
    }

    public static void registerPendingMessage(Context context, String destinationAddress, String text) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("address", destinationAddress);
        contentValues.put("body", text);
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_PENDING);
        context.getContentResolver().insert(Uri.parse(Telephony.Sms.Outbox.CONTENT_URI.toString()), contentValues);
    }

}
