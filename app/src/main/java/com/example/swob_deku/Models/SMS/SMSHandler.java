package com.example.swob_deku.Models.SMS;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SMSHandler {
    static final short DATA_TRANSMISSION_PORT = 8901;

    public static final Uri smsUri = Telephony.Sms.CONTENT_URI;

    public static final Uri inboxContentUri = Telephony.Sms.Inbox.CONTENT_URI;
    public static final Uri outboxContentUri = Telephony.Sms.Outbox.CONTENT_URI;
    public static final Uri sentContentUri = Telephony.Sms.Sent.CONTENT_URI;

    public static void sendSMS(Context context, String destinationAddress, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        try {
            if(data == null)
                return;

            // registerPendingMessage(context, destinationAddress, text, messageId);
            // TODO: Handle sending multipart messages
            if(BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Sending data: " + data);

//            registerPendingMessage(context, destinationAddress, new String(data, StandardCharsets.UTF_8), messageId);
            registerPendingMessage(context, destinationAddress, Base64.encodeToString(data, Base64.DEFAULT), messageId);
            smsManager.sendDataMessage(
                    destinationAddress,
                    null,
                    DATA_TRANSMISSION_PORT,
                    data,
                    sentIntent,
                    deliveryIntent);
            if(BuildConfig.DEBUG) {
                Log.d(SMSHandler.class.getName(), "Data message sent...");
            }
        }
        catch(Throwable e) {
            // throw new IllegalArgumentException(e);
            throw e;
        }
    }

    public static String sendSMS(Context context, String destinationAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
            context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        String threadId = "";
        try {
            if(text.isEmpty() || destinationAddress.isEmpty())
                return "";

            try {
                threadId = registerPendingMessage(context, destinationAddress, text, messageId);
            } catch(Exception e ) {
                e.printStackTrace();
            }

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

        return threadId;
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

        Cursor smsMessagesCursor = context.getContentResolver().query(
                smsUri,
                new String[] { Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON, Telephony.TextBasedSmsColumns.DATE,Telephony.TextBasedSmsColumns.BODY, Telephony.TextBasedSmsColumns.TYPE },
                "address like ?",
                new String[] { "%" + address},
                "date ASC");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSForThread(Context context, String threadId) {
        String[] selection = new String[] { Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.PERSON,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.TYPE };

        Cursor smsMessagesCursor = context.getContentResolver().query(
                smsUri,
                selection,
                Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                new String[] { threadId },
                "date DESC");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSForThreading(Context context) {
        String[] projection = new String[] {
                Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.BODY, Telephony.TextBasedSmsColumns.TYPE, "MAX(date) as date"};

        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "thread_id IS NOT NULL) GROUP BY (thread_id",
                null,
                "date DESC");
    }

    public static Cursor fetchSMSMessagesForSearch(Context context, String searchInput) {
        Uri targetedURI = Telephony.Sms.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[] { Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON, Telephony.TextBasedSmsColumns.DATE,Telephony.TextBasedSmsColumns.BODY, Telephony.TextBasedSmsColumns.TYPE },
                "body like '%" + searchInput + "%'",
                null,
                "date DESC");

        return cursor;
    }

    public static Cursor fetchSMSMessageForAllIds(Context context, ArrayList<Long> messageIds) {
        Uri targetedURI = Telephony.Sms.Inbox.CONTENT_URI;
        String selection = "_id=?";
        String[] selectionArgs = new String[messageIds.size()];
        selectionArgs[0] = String.valueOf(messageIds.get(0));

        for(int i=1;i<messageIds.size(); ++i) {
            selection += " OR _id=?";
            selectionArgs[i] = String.valueOf(messageIds.get(i));
        }

        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[] { Telephony.Sms._ID,
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE },
                selection,
                selectionArgs,
                "date DESC");

        return cursor;
    }

    public static Cursor fetchSMSMessageThreadIdFromMessageId(Context context, long messageId) {
        Cursor cursor = context.getContentResolver().query(
                smsUri,
                 new String[] { Telephony.Sms._ID,
                         Telephony.TextBasedSmsColumns.THREAD_ID,
                         Telephony.TextBasedSmsColumns.ADDRESS,
                         Telephony.TextBasedSmsColumns.PERSON,
                         Telephony.TextBasedSmsColumns.DATE,
                         Telephony.TextBasedSmsColumns.BODY,
                         Telephony.TextBasedSmsColumns.TYPE },
                Telephony.Sms._ID + "=?",
                new String[] { String.valueOf(messageId)},
                "date DESC");

        return cursor;
    }

    public static long registerIncomingMessage(Context context, String address, String body) {
        long messageId = Helpers.generateRandomNumber();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

        try {
            context.getContentResolver().insert(inboxContentUri, contentValues);
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

        context.getContentResolver().update(smsUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerDeliveredMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_COMPLETE);

        context.getContentResolver().update(sentContentUri, contentValues, "_id=?",
                new String[] { Long.toString(messageId)});
    }

    public static void registerSentMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.Sms.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
        contentValues.put(Telephony.Sms.STATUS, Telephony.TextBasedSmsColumns.STATUS_NONE);

        try {
            context.getContentResolver().update(
                    smsUri,
                    contentValues,
                    Telephony.Sms._ID + "=?",
                    new String[] {String.valueOf(messageId)});
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
    }

    public static String registerPendingMessage(Context context, String destinationAddress, String text, long messageId) {
        if(BuildConfig.DEBUG)
            Log.d(SMSHandler.class.getName(), "sending message id: " + messageId);
        String threadId = "";
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_PENDING);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(outboxContentUri, contentValues);

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] {Telephony.TextBasedSmsColumns.THREAD_ID},
                    null,
                    null,
                    null);

            if(cursor.moveToFirst()) {
                threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return threadId;
    }

    public static boolean hasUnreadMessages(Context context, String threadId) {
        try {
            Cursor cursor = context.getContentResolver().query(
                    inboxContentUri,
                    new String[] { Telephony.TextBasedSmsColumns.READ, Telephony.TextBasedSmsColumns.THREAD_ID },
//                    "read=? AND thread_id =? AND type != ?",
                    Telephony.TextBasedSmsColumns.READ + "=? AND " +
                            Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND " +
                            Telephony.TextBasedSmsColumns.TYPE + "!=?",
                    new String[] { "0",
                            String.valueOf(threadId),
                            String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT)},
                    "date DESC LIMIT 1");

            return cursor.getCount() > 0;
        }
        catch(Exception e ) {
            e.printStackTrace();
        }

        return false;
    }

    public static void updateThreadMessagesThread(Context context, String threadId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.READ, Telephony.Sms.READ);
        try {
            context.getContentResolver().update(
                    inboxContentUri,
                    contentValues,
                    Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND " + Telephony.TextBasedSmsColumns.READ +"=?",
                    new String[] { threadId, "0" });
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
    }

    public static List<SMS> dateSegmentations(List<SMS> smsList) {


        List<SMS> copysmsList = new ArrayList<>(smsList);

        for(int i=smsList.size() - 1, j = i; i> -1; --i, --j) {
            SMS currentSMS = smsList.get(i);
            if(BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "sms date: " + currentSMS.getDate() + " - " + currentSMS.getBody());

            Date date = new Date(Long.parseLong(currentSMS.getDate()));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            if(i == smsList.size() - 1 ) {
                copysmsList.add(new SMS(currentSMS.getDate()));
                ++j;
            }
            else {
                Date previousDate = new Date(Long.parseLong(smsList.get(i + 1).getDate()));
                Calendar prevCalendar = Calendar.getInstance();
                prevCalendar.setTime(previousDate);
                if(BuildConfig.DEBUG)
                    Log.d(SMSHandler.class.getName(), prevCalendar.getTime().toString() + ":" + calendar.getTime().toString());

                if (prevCalendar.get(Calendar.HOUR) < calendar.get(Calendar.HOUR)) {
                    copysmsList.add(j, new SMS(currentSMS.getDate()));
                    ++j;
                }
            }
        }

        return copysmsList;
    }

}
