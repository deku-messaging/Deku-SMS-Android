package com.example.swob_deku.Models;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.swob_deku.Commons.Helpers;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSHandler {

    public static void sendSMS(Context context, String destinationAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
            context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        try {
            if(text.isEmpty() || destinationAddress.isEmpty())
                return;

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
                Telephony.Sms.CONTENT_URI,
                new String[] { "_id", "thread_id", "address", "person", "date","body", "type" },
                "address",
                new String[] { address },
                "date ASC LIMIT 1");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesThread(Context context, String threadId) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                 new String[] { "_id", "thread_id", "address", "person", "date","body", "type", "read", "status", "reply_path_present"},
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
                "date DESC");

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


    public static long registerIncomingMessage(Context context, String address, String body) {
        long messageId = Helpers.generateRandomNumber();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

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
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_FAILED);
        contentValues.put(Telephony.TextBasedSmsColumns.ERROR_CODE, errorCode);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);

        Uri failedContentUri = Telephony.Sms.CONTENT_URI;
        context.getContentResolver().update(failedContentUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerDeliveredMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_COMPLETE);

        Uri failedContentUri = Telephony.Sms.CONTENT_URI;
        context.getContentResolver().update(failedContentUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerSentMessage(Context context, long messageId) {
        // TODO: try updating this from pending messages rather than deleting and reinserting
        Uri inboxContentUri = Telephony.Sms.Sent.CONTENT_URI;
        Uri outboxContentUri = Telephony.Sms.CONTENT_URI;

        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.Sms.STATUS, Telephony.TextBasedSmsColumns.STATUS_NONE);

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

            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

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
        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_PENDING);
        Uri outboxContentUri = Telephony.Sms.Outbox.CONTENT_URI;
        try {
            context.getContentResolver().insert(outboxContentUri, contentValues);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasUnreadMessages(Context context, String threadId) {
        try {
            Cursor cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[] { Telephony.TextBasedSmsColumns.READ, Telephony.TextBasedSmsColumns.THREAD_ID },
                    "read=? AND thread_id =?",
                    new String[] { "0", String.valueOf(threadId)}, "date DESC LIMIT 1");

            return cursor.getCount() > 0;
        }
        catch(Exception e ) {
            e.printStackTrace();
        }

        return false;
    }

    public static void updateSMSMessagesThreadStatus(Context context, String threadId, String read) {
//        Cursor cursor = fetchSMSMessagesThread(context, threadId);

//        if(cursor.moveToFirst()) {
//            do {
//                SMS sms = new SMS(cursor);
//                Log.d("", "updating: " + sms.getBody() + " - " + sms.getId() + " | " + read);
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(Telephony.TextBasedSmsColumns.READ, read);
//                try {
//                    context.getContentResolver().update(
//                            Telephony.Sms.Inbox.CONTENT_URI,
//                            contentValues,
//                            "_id=? AND read=?",
//                            new String[] { String.valueOf(sms.getId()), "0" });
//                }
//                catch(Exception e ) {
//                    e.printStackTrace();
//                }
//            } while(cursor.moveToNext());
//        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.READ, read);
        try {
            context.getContentResolver().update(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    contentValues,
                    "thread_id=? AND read=?",
                    new String[] { threadId, "0" });
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
    }

    public static List<SMS> appendDateIndexes(List<SMS> smsMessages ) {
        List<SMS> appendedList = new ArrayList<>();
        Date previousDate = null;

        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();

        DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        for (SMS sms : smsMessages) {
            Date date = new Date(Long.parseLong(sms.getDate()));
            if(previousDate != null) {
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

        String dateStr = dateFormat.format(previousDate);
        appendedList.add(new SMS(dateStr));
        return appendedList;
    }
}
