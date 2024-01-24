package com.afkanerd.deku.DefaultSMS.Models;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

public class NativeSMSDB {
    public static String ID = "ID";

    public static int THREAD_ID = 0;
    public static int MESSAGE_ID = 1;

    public static int BODY = 2;

    public static int ADDRESS = 3;
    public static int SUBSCRIPTION_ID = 4;
    public static int DATE_SENT = 5;
    public static int DATE = 6;

    public static Cursor fetchAll(Context context) {
        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                "thread_id IS NOT NULL",
                null,
                null);
    }
    public static Cursor fetchByThreadId(Context context, String threadId) {
        return context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                null,
                Telephony.Sms.THREAD_ID + "=?",
                new String[]{threadId},
                null);
    }

    public static Cursor fetchByMessageId(@NonNull Context context, String id) {
        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                Telephony.Sms._ID + "=?",
                new String[]{id},
                null);
    }

    public static int deleteMultipleMessages(Context context, String[] ids) {
        return context.getContentResolver().delete(
                Telephony.Sms.CONTENT_URI,
                Telephony.Sms._ID + " in (" +
                        TextUtils.join(",", Collections.nCopies(ids.length, "?")) + ")", ids);
    }

    public static int deleteThreads(Context context, String[] threadIds) {
        return context.getContentResolver().delete(
                Telephony.Sms.CONTENT_URI,
                Telephony.TextBasedSmsColumns.THREAD_ID + " in (" +
                        TextUtils.join(",", Collections.nCopies(threadIds.length, "?")) + ")", threadIds);
    }

    protected static int deleteMessage(Context context, String messageId) {
        try {
            return context.getContentResolver().delete(
                    Telephony.Sms.CONTENT_URI,
                    Telephony.Sms._ID + " = ?", new String[]{messageId});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    protected static int deleteTypeForThread(Context context, String type, String threadId) {
        try {
            return context.getContentResolver().delete(
                    Telephony.Sms.CONTENT_URI,
                    Telephony.TextBasedSmsColumns.THREAD_ID + " = ? AND " +
                            Telephony.TextBasedSmsColumns.TYPE + " = ?",
                    new String[]{threadId, type});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    protected static int deleteAllType(Context context, String type) {
        try {
            return context.getContentResolver().delete(
                    Telephony.Sms.CONTENT_URI,
                    Telephony.TextBasedSmsColumns.TYPE + " = ?",
                    new String[]{type});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /*
     * Places which require playing with Native SMS DB
     * Outgoing:
     *  Actions:
     *      - Manual
     *          - From send message
     *          - From notifications - X
     *  Broadcast:
     *      - state changes
     *          - Auto
     *              - pending - X
     *              - sent - X
     *              - failed - X
     *              - delivered -X
     *
     * Incoming:
     *  Actions:
     *      - Manual:
     *          - Read status
     */

    private static String[] parseNewIncomingUriForThreadInformation(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.Sms._ID},
                null,
                null,
                null);
        Log.d(NativeSMSDB.class.getName(), "Parsing draft information: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            String threadId = cursor.getString(
                    cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            String messageId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
            cursor.close();
            return new String[]{threadId, messageId};
        }

        return null;
    }

    private static String[] broadcastStateChanged(Context context, String messageId) {
        /**
         * Threads ID
         * Message ID
         */
        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.Sms._ID},
                Telephony.Sms._ID + "=?",
                new String[]{messageId},
                null);

        if (cursor.moveToFirst()) {
            String threadId = cursor.getString(
                    cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            cursor.close();
            return new String[]{threadId, messageId};
        }

        return null;
    }

    public static class Outgoing {

        private static int update_status(Context context, int statusCode, String messageId, int errorCode) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.TextBasedSmsColumns.STATUS, statusCode);

            if(statusCode == Telephony.Sms.STATUS_NONE)
                contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);

            if(statusCode == Telephony.Sms.STATUS_FAILED || errorCode > -1) {
                contentValues.put(Telephony.TextBasedSmsColumns.ERROR_CODE, errorCode);
            }

            try {
                return context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        "_id=?",
                        new String[]{messageId});
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        protected static String[] _send_text(Context context, String messageId, String destinationAddress,
                                                 String text, int subscriptionId, Bundle bundle) throws Exception {
            String[] pendingOutputs = register_pending(context, messageId, destinationAddress, text, subscriptionId);
            PendingIntent[] pendingIntents = getPendingIntents(context, messageId, bundle);
            Transmissions.sendTextSMS(destinationAddress, text,
                    pendingIntents[0], pendingIntents[1], subscriptionId);
            return pendingOutputs;
        }


        protected static String[] _send_data(Context context, String messageId, String destinationAddress,
                                             byte[] data, int subscriptionId, Bundle bundle) throws Exception {
            if(subscriptionId < 0) {
                Toast.makeText(context, "Subscription id not set!", Toast.LENGTH_LONG).show();
                return null;
            }

            String[] outputs = register_pending_data(context, messageId, destinationAddress,
                    Base64.encodeToString(data, Base64.DEFAULT), subscriptionId);
            PendingIntent[] pendingIntents = getPendingIntentsForData(context, messageId, bundle);
            Transmissions.sendDataSMS(destinationAddress, data,
                    pendingIntents[0], pendingIntents[1], subscriptionId);
            return outputs;
        }

        protected static void _send_key(Context context, String messageId, String destinationAddress,
                                             byte[] data, int subscriptionId, Bundle bundle) throws Exception {
            PendingIntent[] pendingIntents = getPendingIntentsForData(context, messageId, bundle);
            Transmissions.sendDataSMS(destinationAddress, data,
                    pendingIntents[0], pendingIntents[1], subscriptionId);
        }

        private static String[] register_pending_data(Context context, String messageId,
                                                 String destinationAddress, String text, int subscriptionId) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
            contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                    Telephony.TextBasedSmsColumns.STATUS_PENDING);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
            contentValues.put(Telephony.TextBasedSmsColumns.DATE,
                    String.valueOf(System.currentTimeMillis()));

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                return parseNewIncomingUriForThreadInformation(context, uri);

            } catch (Exception e) {
                throw e;
            }
        }

        private static String[] register_pending(Context context, String messageId,
                                                 String destinationAddress, String text, int subscriptionId) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
            contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                    Telephony.TextBasedSmsColumns.STATUS_PENDING);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);
            contentValues.put(Telephony.TextBasedSmsColumns.DATE_SENT,
                    String.valueOf(System.currentTimeMillis()));

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                return parseNewIncomingUriForThreadInformation(context, uri);

            } catch (Exception e) {
                throw e;
            }
        }

        protected static String[] register_drafts(Context context, String messageId,
                                                 String destinationAddress, String text, int subscriptionId) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
            contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                    Telephony.TextBasedSmsColumns.STATUS_PENDING);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);
            contentValues.put(Telephony.TextBasedSmsColumns.DATE_SENT,
                    String.valueOf(System.currentTimeMillis()));

            Uri uri = context.getContentResolver().insert(
                    Telephony.Sms.CONTENT_URI,
                    contentValues);
            return parseNewIncomingUriForThreadInformation(context, uri);
        }

        public static String[] register_failed(Context context, String messageId, int errorCode) {
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_FAILED,
                            messageId, errorCode);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static String[] register_delivered(@NonNull Context context, String messageId) {
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_COMPLETE,
                            messageId, -1);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static String[] register_sent(Context context, String messageId) {
            Log.d(NativeSMSDB.class.getName(), "Registered sent message");
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_NONE,
                            messageId, -1);
            Log.d(NativeSMSDB.class.getName(), "Registered sent message update: " + numberChanged);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static PendingIntent[] getPendingIntentsForData(Context context, String messageId, Bundle bundle) {
            Intent sentIntent = new Intent(IncomingDataSMSBroadcastReceiver.DATA_SENT_BROADCAST_INTENT);
            sentIntent.setPackage(context.getPackageName());
            sentIntent.putExtra(ID, messageId);

            Intent deliveredIntent = new Intent(IncomingDataSMSBroadcastReceiver.DATA_DELIVERED_BROADCAST_INTENT);
            deliveredIntent.setPackage(context.getPackageName());
            deliveredIntent.putExtra(Conversation.ID, messageId);

            if(bundle != null) {
                sentIntent.putExtras(bundle);
                deliveredIntent.putExtras(bundle);
            }

            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                    (int)Long.parseLong(messageId),
                    sentIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                    (int)Long.parseLong(messageId),
                    deliveredIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
        }

        public static PendingIntent[] getPendingIntents(Context context, String messageId, Bundle bundle) {
            Intent sentIntent = new Intent(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT);
            sentIntent.setPackage(context.getPackageName());
            sentIntent.putExtra(ID, messageId);

            Intent deliveredIntent = new Intent(IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT);
            deliveredIntent.setPackage(context.getPackageName());
            deliveredIntent.putExtra(Conversation.ID, messageId);

            if(bundle != null) {
                sentIntent.putExtras(bundle);
                deliveredIntent.putExtras(bundle);
            }

            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                    (int)Long.parseLong(messageId),
                    sentIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                    (int)Long.parseLong(messageId),
                    deliveredIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
        }


    }


    public static class Incoming {

        public static int update_all_read(Context context, int read, String[] threadId) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.Sms.READ, read);

            try {
                return context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        "thread_id in (" + TextUtils.join(",",
                                Collections.nCopies(threadId.length, "?")) + ")",
                        threadId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        public static int update_all_read(Context context, int read) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.Sms.READ, read);

            try {
                return context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        null,
                        null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        public static int update_read(Context context, int read, String thread_id, String messageId) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.Sms.READ, read);

            try {
                int updated = context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        "thread_id=?",
                        new String[]{thread_id});
                Log.d(NativeSMSDB.class.getName(), "Updated to read: " + updated);
                return updated;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        public static String[] register_incoming_text(Context context, Intent intent) throws IOException {
            long messageId = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();

            Bundle bundle = intent.getExtras();
            int subscriptionId = bundle.getInt("subscription", -1);

            String address = "";
            StringBuilder bodyBuffer = new StringBuilder();
            long dateSent = 0;
            long date = System.currentTimeMillis();

            for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                address = currentSMS.getDisplayOriginatingAddress();
//                if(BuildConfig.DEBUG) {
//                    Log.d(NativeSMSDB.class.getName(), "Incoming Display address: " + address);
//                    Log.d(NativeSMSDB.class.getName(), "Incoming Originating address: " + currentSMS.getOriginatingAddress());
//                    Log.d(NativeSMSDB.class.getName(), "Incoming sent date: " + currentSMS.getTimestampMillis());
//                    Log.d(NativeSMSDB.class.getName(), "Incoming is reply: " + currentSMS.isReplyPathPresent());
//                    Log.d(NativeSMSDB.class.getName(), "Incoming is status reply: " + currentSMS.isStatusReportMessage());
//                }

                String text_message = currentSMS.getDisplayMessageBody();

                dateSent = currentSMS.getTimestampMillis();

                bodyBuffer.append(text_message);
            }
            String body = bodyBuffer.toString();

            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.DATE_SENT, dateSent);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                Log.d(NativeSMSDB.class.getName(), "URI: " + uri.toString());
                String[] broadcastOutputs = parseNewIncomingUriForThreadInformation(context, uri);
                String[] returnString = new String[7];
                returnString[THREAD_ID] = broadcastOutputs[THREAD_ID];
                returnString[MESSAGE_ID] = broadcastOutputs[MESSAGE_ID];
                returnString[BODY] = body;
                returnString[ADDRESS] = address;
                returnString[SUBSCRIPTION_ID] = String.valueOf(subscriptionId);
                returnString[DATE_SENT] = String.valueOf(dateSent);
                returnString[DATE] = String.valueOf(date);
                return returnString;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        public static String[] register_incoming_data(Context context, Intent intent) throws IOException {
            long messageId = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();

            Bundle bundle = intent.getExtras();
            int subscriptionId = bundle.getInt("subscription", -1);

            String address = "";
            ByteArrayOutputStream dataBodyBuffer = new ByteArrayOutputStream();

            long dateSent = 0;
            long date = System.currentTimeMillis();

            for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                address = currentSMS.getDisplayOriginatingAddress();

                dataBodyBuffer.write(currentSMS.getUserData());
                dateSent = currentSMS.getTimestampMillis();
            }

            String body = Base64.encodeToString(dataBodyBuffer.toByteArray(), Base64.DEFAULT);
            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
//            contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                String[] broadcastOutputs = parseNewIncomingUriForThreadInformation(context, uri);
                String[] returnString = new String[7];
                returnString[THREAD_ID] = broadcastOutputs[THREAD_ID];
                returnString[MESSAGE_ID] = broadcastOutputs[MESSAGE_ID];
                returnString[BODY] = body;
                returnString[ADDRESS] = address;
                returnString[SUBSCRIPTION_ID] = String.valueOf(subscriptionId);
                returnString[DATE_SENT] = String.valueOf(dateSent);
                returnString[DATE] = String.valueOf(date);
                return returnString;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}
