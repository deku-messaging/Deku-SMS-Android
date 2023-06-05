package com.example.swob_deku.BroadcastReceivers;

import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.KEY_TEXT_REPLY;
import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT;
import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Person;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;
import com.example.swob_deku.Services.RMQConnection;

import java.util.ArrayList;
import java.util.List;

public class IncomingTextSMSReplyActionBroadcastReceiver extends BroadcastReceiver {
    public static String BROADCAST_STATE = BuildConfig.APPLICATION_ID + ".BROADCAST_STATE";
    public static String SENT_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".SENT_BROADCAST_INTENT";
    public static String DELIVERED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".DELIVERED_BROADCAST_INTENT";
    public static String REPLY_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".REPLY_BROADCAST_ACTION";
    public static String MARK_AS_READ_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".MARK_AS_READ_BROADCAST_ACTION";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(getClass().getName(), "Received manifest broadcast now: " + intent.getAction());
        if(intent.getAction().equals(REPLY_BROADCAST_INTENT)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);
                String address = intent.getStringExtra(SMSSendActivity.ADDRESS);
                String threadId = intent.getStringExtra(SMSSendActivity.THREAD_ID);

                Log.d(getClass().getName(), "Yep broadcast is called with reply: " + reply.toString());
                Log.d(getClass().getName(), "Yep broadcast is called with address: " + address);

                try {
                    long messageId = Helpers.generateRandomNumber();
                    PendingIntent[] pendingIntents = IncomingTextSMSBroadcastReceiver.getPendingIntents(context, messageId);
                    SMSHandler.sendTextSMS(context, address, reply.toString(),
                            pendingIntents[0], pendingIntents[1], messageId, null);

                    List<NotificationCompat.MessagingStyle.Message> messages = new ArrayList<>();
                    messages.add(new NotificationCompat.MessagingStyle.Message(reply,
                            System.currentTimeMillis(),
                            context.getString(R.string.notification_title_reply_you)));

                    Cursor cursor = SMSHandler.fetchUnreadSMSMessagesForThreadId(context, threadId);

                    Intent receivedSmsIntent = new Intent(context, SMSSendActivity.class);
                    receivedSmsIntent.putExtra(SMSSendActivity.ADDRESS, address);
                    receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity( context,
                            Integer.parseInt(threadId),
                            receivedSmsIntent, PendingIntent.FLAG_IMMUTABLE);
                    NotificationCompat.Builder builder = IncomingTextSMSBroadcastReceiver
                            .getNotificationHandler(context, cursor, messages, intent,
                                    Integer.parseInt(String.valueOf(messageId)), threadId)
                                    .setContentIntent(pendingReceivedSmsIntent);
                    cursor.close();

                    // Issue the new notification.
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    notificationManager.notify(Integer.parseInt(threadId), builder.build());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else if(intent.getAction().equals(MARK_AS_READ_BROADCAST_INTENT)) {
            String threadId = intent.getStringExtra(SMSSendActivity.THREAD_ID);
            try {
                SMSHandler.updateMarkThreadMessagesAsRead(context, threadId);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(Integer.parseInt(threadId));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        else if(intent.getAction().equals(SMS_SENT_BROADCAST_INTENT)) {
            long id = intent.getLongExtra(SMSSendActivity.ID, -1);
            intent.putExtra(BROADCAST_STATE, SENT_BROADCAST_INTENT);
            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    try {
                        SMSHandler.registerSentMessage(context, id);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY:
                case SmsManager.RESULT_RIL_NETWORK_ERR:
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                default:
                    try {
                        SMSHandler.registerFailedMessage(context, id, getResultCode());
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
            }
        }
        else if(intent.getAction().equals(SMS_DELIVERED_BROADCAST_INTENT)) {
            intent.putExtra(BROADCAST_STATE, DELIVERED_BROADCAST_INTENT);
            long id = intent.getLongExtra(SMSSendActivity.ID, -1);
            if (getResultCode() == Activity.RESULT_OK) {
                SMSHandler.registerDeliveredMessage(context, id);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                            + getResultCode());
            }
        }

        Intent newIntent = new Intent(SMSHandler.MESSAGE_STATE_CHANGED_BROADCAST_INTENT);
        newIntent.putExtras(intent.getExtras());

        context.sendBroadcast(newIntent);
    }
}
