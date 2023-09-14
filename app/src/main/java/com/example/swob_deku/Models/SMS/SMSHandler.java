package com.example.swob_deku.Models.SMS;

import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;


import androidx.annotation.NonNull;

import com.example.swob_deku.BroadcastReceivers.OutgoingDataSMSBroadcastReceiver;
import com.example.swob_deku.BroadcastReceivers.OutgoingTextSMSBroadcastReceiver;
import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.RMQ.RMQConnection;
import com.example.swob_deku.Models.Security.SecurityHelpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SMSHandler {
    public static final int ASCII_MAGIC_NUMBER = 127;

    public static final Uri SMS_CONTENT_URI = Telephony.Sms.CONTENT_URI;

    public static final Uri SMS_INBOX_CONTENT_URI = Telephony.Sms.Inbox.CONTENT_URI;
    public static final Uri SMS_OUTBOX_CONTENT_URI = Telephony.Sms.Outbox.CONTENT_URI;
    public static final Uri SMS_SENT_CONTENT_URI = Telephony.Sms.Sent.CONTENT_URI;

    public static final String DATA_SMS_WORK_MANAGER_TAG_NAME = "DATA_SMS_ROUTING";

    public static final String MESSAGE_STATE_CHANGED_BROADCAST_INTENT =
            "MESSAGE_STATE_CHANGED_BROADCAST_INTENT";

    public static void deleteThreads(Context context, String[] ids) {
        try {
            int updateCount = context.getContentResolver().delete(SMS_CONTENT_URI,
                    Telephony.TextBasedSmsColumns.THREAD_ID + " in (" +
                            TextUtils.join(",", Collections.nCopies(ids.length, "?")) + ")", ids);

            if (BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Deleted outbox: " + updateCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSameMinute(SMS sms1, SMS sms2) {
        Date date = new Date(Long.parseLong(sms1.getDate()));
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(date);

        String previousDateString = sms2.getDate();
        Date previousDate = new Date(Long.parseLong(previousDateString));
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.setTime(previousDate);

        return !((prevCalendar.get(Calendar.HOUR_OF_DAY) != currentCalendar.get(Calendar.HOUR_OF_DAY)
                || (prevCalendar.get(Calendar.MINUTE) != currentCalendar.get(Calendar.MINUTE))
                || (prevCalendar.get(Calendar.DATE) != currentCalendar.get(Calendar.DATE))));
    }

    public static boolean isSameHour(SMS sms1, SMS sms2) {
        Date date = new Date(Long.parseLong(sms1.getDate()));
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(date);

        String previousDateString = sms2.getDate();
        Date previousDate = new Date(Long.parseLong(previousDateString));
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.setTime(previousDate);

        return !((prevCalendar.get(Calendar.HOUR_OF_DAY) != currentCalendar.get(Calendar.HOUR_OF_DAY)
                || (prevCalendar.get(Calendar.DATE) != currentCalendar.get(Calendar.DATE))));
    }

    public static Cursor fetchSMSForImagesByRIL(@NonNull Context context, String RIL) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE, Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                Telephony.TextBasedSmsColumns.BODY + " like ?",
                new String[]{RIL + "%"},
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSInboxById(@NonNull Context context, String id) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE, Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                Telephony.Sms._ID + "=?",
                new String[]{id},
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchAllMessages(@NonNull Context context) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE, Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                null,
                null,
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSForThreading(Context context) {
        String[] projection = new String[]{
                Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.READ,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.BODY,
//                Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID,
                Telephony.TextBasedSmsColumns.TYPE,
                "MAX(date) as date"};

        return context.getContentResolver().query(
                SMS_CONTENT_URI,
                projection,
                "thread_id IS NOT NULL) GROUP BY (thread_id",
                null,
                "date DESC");
    }

    public static Cursor fetchSMSMessagesForSearch(Context context, String searchInput) {
        Uri targetedURI = Telephony.Sms.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[]{Telephony.Sms._ID,
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                "body like '%" + searchInput + "%') GROUP BY (thread_id",
                null,
                "date DESC");

        return cursor;
    }

    public static Cursor fetchSMSMessageForAllIds(Context context, ArrayList<Long> messageIds) {
        Uri targetedURI = Telephony.Sms.CONTENT_URI;
        String selection = "_id=?";
        String[] selectionArgs = new String[messageIds.size()];
        selectionArgs[0] = String.valueOf(messageIds.get(0));

        for (int i = 1; i < messageIds.size(); ++i) {
            selection += " OR _id=?";
            selectionArgs[i] = String.valueOf(messageIds.get(i));
        }

        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[]{Telephony.Sms._ID,
                        Telephony.TextBasedSmsColumns.STATUS,
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                selection,
                selectionArgs,
                "date DESC");

        return cursor;
    }

    public static long registerIncomingMessage(Context context, String address, String body, String subscriptionId) {
        long messageId = Helpers.generateRandomNumber();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
        contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

        try {
            context.getContentResolver().insert(SMS_INBOX_CONTENT_URI, contentValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageId;
    }

    public static void registerFailedMessage(Context context, long messageId, int errorCode) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_FAILED);
        contentValues.put(Telephony.TextBasedSmsColumns.ERROR_CODE, errorCode);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);

        try {
            context.getContentResolver().update(SMS_SENT_CONTENT_URI, contentValues, "_id=?",
                    new String[]{Long.toString(messageId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerDeliveredMessage(@NonNull Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_COMPLETE);

        try {
            context.getContentResolver().update(
                    SMS_SENT_CONTENT_URI,
                    contentValues,
                    Telephony.Sms._ID + "=?",
                    new String[]{Long.toString(messageId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerSentMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);

        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_NONE);

        try {
            context.getContentResolver().update(
                    SMS_SENT_CONTENT_URI,
                    contentValues,
                    Telephony.Sms._ID + "=?",
                    new String[]{Long.toString(messageId)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static final String SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST";

    public static final String SMS_NEW_DATA_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_DATA_REGISTERED_PENDING_BROADCAST";

    public static final String SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST";
    public static String SMS_SENT_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_SENT_BROADCAST_INTENT";
    public static String SMS_DELIVERED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_DELIVERED_BROADCAST_INTENT";

    public static String registerPendingServerMessage(Context context, String destinationAddress,
                                                String text, int subscriptionId, String messageSid) {
        long messageId = Helpers.generateRandomNumber();

        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_PENDING);
        contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

        try {
            Uri uri = context.getContentResolver().insert(
                    SMS_OUTBOX_CONTENT_URI,
                    contentValues);

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{Telephony.TextBasedSmsColumns.THREAD_ID},
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                String threadId = cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));

                Intent broadcastIntent = new Intent(context, OutgoingTextSMSBroadcastReceiver.class);

                broadcastIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                broadcastIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);
//                broadcastIntent.putExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY, globalMessageKey);
                broadcastIntent.putExtra(RMQConnection.MESSAGE_SID, messageSid);
                broadcastIntent.setAction(SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST);

                context.sendBroadcast(broadcastIntent);
                cursor.close();

                return threadId;
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    /**
     * This module would send out a broadcast informing the outgoing message modules that there is a
     * new message available for sending.
     * @param context
     * @param destinationAddress
     * @param text
     * @param subscriptionId
     * @return String
     */
    public static String registerPendingMessage(Context context, String destinationAddress,
                                              String text, int subscriptionId) {
        long messageId = Helpers.generateRandomNumber();

        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_PENDING);
        contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

        try {
            Uri uri = context.getContentResolver().insert(
                    SMS_OUTBOX_CONTENT_URI,
                    contentValues);

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{Telephony.TextBasedSmsColumns.THREAD_ID},
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                String threadId = cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));

                Intent broadcastIntent = new Intent(context, OutgoingTextSMSBroadcastReceiver.class);

                broadcastIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                broadcastIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);
                broadcastIntent.setAction(SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST);

                context.sendBroadcast(broadcastIntent);
                cursor.close();

                return threadId;
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    /**
     * This module would send out a broadcast informing the outgoing message modules that there is a
     * new message available for sending.
     * @param context
     * @param destinationAddress
     * @param data
     * @param subscriptionId
     * @param data: if true sends the message as a data SMS, if false sends as a text message
     * @return String
     */
    public static String registerPendingKeyMessage(Context context, String destinationAddress,
                                                byte[] data, int subscriptionId) {
        long messageId = Helpers.generateRandomNumber();

        String text = SecurityHelpers.FIRST_HEADER
                + Base64.encodeToString(data, Base64.DEFAULT)
                + SecurityHelpers.END_HEADER;


        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_PENDING);
        contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

        try {
            Uri uri = context.getContentResolver().insert(
                    SMS_OUTBOX_CONTENT_URI,
                    contentValues);

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{Telephony.TextBasedSmsColumns.THREAD_ID},
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                String threadId = cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));

                Intent broadcastIntent = new Intent(context, OutgoingDataSMSBroadcastReceiver.class);
                broadcastIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                broadcastIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);
                broadcastIntent.setAction(SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST);

                context.sendBroadcast(broadcastIntent);
                cursor.close();

                return threadId;
            }
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    public static void broadcastMessageStateChanged(Context context, Intent intent){
        Intent newIntent = new Intent(SMSHandler.MESSAGE_STATE_CHANGED_BROADCAST_INTENT);

        if(intent != null) {
            newIntent.putExtras(intent.getExtras());
            Log.d(SMSHandler.class.getName(), "NewIntent broadcast with global key: "
                    + newIntent.getStringExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY));
        }

        context.sendBroadcast(newIntent);
    }

    public static String calculateSMS(String message) {
        int numberOfMessages = (int) Math.ceil(message.length() / 140f);
        int sizeIntoNewMessage = (140 * numberOfMessages) - message.length();

        return sizeIntoNewMessage + "/" + numberOfMessages;
    }

    public static int updateMarkThreadMessagesAsRead(Context context, String threadId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.READ, "1");
        try {
            int updateCount = context.getContentResolver().update(
                    SMS_CONTENT_URI,
                    contentValues,
                    Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND " + Telephony.TextBasedSmsColumns.READ + "=?",
                    new String[]{threadId, "0"});

            if (BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Updated read for: " + updateCount);
            return updateCount;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static void updateMessage(Context context, String messageId, String body) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
        try {
            int updateCount = context.getContentResolver().update(
                    SMS_INBOX_CONTENT_URI,
                    contentValues,
                    Telephony.Sms._ID + "=? ",
                    new String[]{messageId});

            if (BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Updated read for: " + updateCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int calculateOffset(Context context, String threadId, String messageId) {
        Cursor cursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[]{Telephony.Sms._ID},
                Telephony.Sms.THREAD_ID + "=?",
                new String[]{threadId},
                "date DESC");

        int offset = -1;
        if (cursor.moveToNext()) {
            do {
                ++offset;
                int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                String id = String.valueOf(cursor.getString(idIndex));

                if (messageId.equals(id)) {
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return offset;
    }

    public static PendingIntent[] getPendingIntents(Context context, long messageId) {
        Intent sentIntent = new Intent(SMS_SENT_BROADCAST_INTENT);
        sentIntent.setPackage(context.getPackageName());
        sentIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_BROADCAST_INTENT);
        deliveredIntent.setPackage(context.getPackageName());
        deliveredIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)),
                sentIntent,
                PendingIntent.FLAG_IMMUTABLE);

        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)),
                deliveredIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
    }

    public static PendingIntent[] getPendingIntentsForServerRequest(Context context, long messageId,
                                                                    String globalMessageId) {
        Intent sentIntent = new Intent(SMS_SENT_BROADCAST_INTENT);
        sentIntent.setPackage(context.getPackageName());
        sentIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);
        sentIntent.putExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY, globalMessageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_BROADCAST_INTENT);
        deliveredIntent.setPackage(context.getPackageName());
        deliveredIntent.putExtra(SMS.SMSMetaEntity.ID, messageId);

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)),
                sentIntent,
                PendingIntent.FLAG_IMMUTABLE);

        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)),
                deliveredIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
    }

}
