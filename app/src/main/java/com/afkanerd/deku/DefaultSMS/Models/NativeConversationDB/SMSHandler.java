package com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB;

import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;


import androidx.annotation.NonNull;

import com.afkanerd.deku.DefaultSMS.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;

public class SMSHandler {
    public static final int ASCII_MAGIC_NUMBER = 127;

    public static final Uri SMS_CONTENT_URI = Telephony.Sms.CONTENT_URI;

    public static final Uri SMS_INBOX_CONTENT_URI = Telephony.Sms.Inbox.CONTENT_URI;
    public static final Uri SMS_OUTBOX_CONTENT_URI = Telephony.Sms.Outbox.CONTENT_URI;
    public static final Uri SMS_SENT_CONTENT_URI = Telephony.Sms.Sent.CONTENT_URI;

    public static final String DATA_SMS_WORK_MANAGER_TAG_NAME = "DATA_SMS_ROUTING";

    public static final String NATIVE_STATE_CHANGED_BROADCAST_INTENT =
            "NATIVE_STATE_CHANGED_BROADCAST_INTENT";

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

    public static Cursor fetchThreads(Context context) {
//        return context.getContentResolver().query(
//                Telephony.Sms.Conversations.CONTENT_URI,
//                null, null, null,
//                "date DESC");

        return context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.THREAD_ID},
                "thread_id IN " +
                        "(SELECT thread_id, type, date FROM sms GROUP BY thread_id ORDER BY date DESC)",
                null,
                "thread_id ASC");
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
//                "body like '%" + searchInput + "%') GROUP BY (thread_id",
                "body like '%" + searchInput + "%'",
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


    public static final String SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_TEXT_REGISTERED_PENDING_BROADCAST";

    public static final String SMS_NEW_DATA_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_DATA_REGISTERED_PENDING_BROADCAST";

    public static final String SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST =
            BuildConfig.APPLICATION_ID + ".SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST";

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

}
