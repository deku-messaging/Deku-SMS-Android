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
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.swob_server.Commons.Helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SMSHandler {

    public static void sendSMS(Context context, String destinationAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
            context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        try {
            registerPendingMessage(context, destinationAddress, text, messageId);
            // TODO: Handle sending multipart messages
            ArrayList<String> dividedMessage = smsManager.divideMessage(text);
            if(dividedMessage.size() < 2 )
                smsManager.sendTextMessage(destinationAddress, null, text, sentIntent, deliveryIntent);
            else {
                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();

                for(int i=0;i<dividedMessage.size() - 1; i++) {
                    sentPendingIntents.add(null);
                    deliveredPendingIntents.add(null);
                }

                sentPendingIntents.add(sentIntent);
                deliveredPendingIntents.add(sentIntent);

                smsManager.sendMultipartTextMessage(
                        destinationAddress,
                        null,
                        dividedMessage, sentPendingIntents, deliveredPendingIntents);
            }
        }
        catch(Throwable e) {
            // throw new IllegalArgumentException(e);
            throw e;
        }
    }

    public static Cursor fetchSMSMessageId(Context context, long id) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                null,
                "_id=?",
                new String[] { Long.toString(id) },
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesAddress(Context context, String address) {
        address = address.replaceAll("[\\s-]", "");
        Log.d("", "Composing to: " + address);

        Cursor smsMessagesCursor = context.getContentResolver().query(
                Uri.parse("content://sms"),
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "address like ?",
                new String[] { "%" + address + "%" },
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

    public static Cursor fetchSMSMessageThreadIdFromMessageId(Context context, long messageId) {
        Uri targetedURI = Telephony.Sms.Inbox.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                 new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "_id=?",
                new String[] { String.valueOf(messageId)},
                null);

        return cursor;
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


    public static long registerIncomingMessage(Context context, SmsMessage smsMessage) {
        long messageId = Helpers.generateRandomNumber();
        ContentValues contentValues = new ContentValues();

        contentValues.put("_id", messageId);
        contentValues.put("address", smsMessage.getOriginatingAddress());
        contentValues.put("body", smsMessage.getMessageBody());
        contentValues.put("type", Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

        try {
            context.getContentResolver().insert(Uri.parse(Telephony.Sms.CONTENT_URI.toString()), contentValues);
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
        return messageId;
    }

    public static void registerFailedMessage(Context context, long messageId, int errorCode) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_FAILED);
        contentValues.put("error_code", errorCode);
        contentValues.put("type", Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);

        Uri failedContentUri = Telephony.Sms.CONTENT_URI;
        context.getContentResolver().update(failedContentUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerDeliveredMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_COMPLETE);

        Uri failedContentUri = Telephony.Sms.CONTENT_URI;
        context.getContentResolver().update(failedContentUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerSentMessage(Context context, long messageId) {
        Uri inboxContentUri = Telephony.Sms.Sent.CONTENT_URI;
        Uri outboxContentUri = Telephony.Sms.CONTENT_URI;

        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", messageId);
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_NONE);

        Cursor cursor = fetchSMSMessageId(context, messageId);
        if(cursor.moveToFirst()) {
            try {
                context.getContentResolver().delete(outboxContentUri, "_id=?",
                        new String[]{Long.toString(messageId)});
            }
            catch (Exception e ) {
                e.printStackTrace();
            }
            SMS sms = new SMS(cursor);
            String destinationAddress = sms.getAddress();
            String text = sms.getBody();

            contentValues.put("address", destinationAddress);
            contentValues.put("body", text);

            try {
                context.getContentResolver().insert(inboxContentUri, contentValues);
            }
            catch(Exception e ) {
                e.printStackTrace();
            }
        }
    }

    public static void registerPendingMessage(Context context, String destinationAddress, String text, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", messageId);
        contentValues.put("address", destinationAddress);
        contentValues.put("body", text);
        contentValues.put("status", Telephony.TextBasedSmsColumns.STATUS_PENDING);
        Uri outboxContentUri = Telephony.Sms.Outbox.CONTENT_URI;
        try {
            context.getContentResolver().insert(outboxContentUri, contentValues);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

}
