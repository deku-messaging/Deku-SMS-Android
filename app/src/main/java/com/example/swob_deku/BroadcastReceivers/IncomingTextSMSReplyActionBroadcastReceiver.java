package com.example.swob_deku.BroadcastReceivers;

import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.KEY_TEXT_REPLY;
import static com.example.swob_deku.Models.SMS.SMSHandler.SMS_DELIVERED_BROADCAST_INTENT;
import static com.example.swob_deku.Models.SMS.SMSHandler.SMS_SENT_BROADCAST_INTENT;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.RMQ.RMQConnection;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;

import java.util.ArrayList;
import java.util.List;

public class IncomingTextSMSReplyActionBroadcastReceiver extends BroadcastReceiver {
    public static String BROADCAST_STATE = BuildConfig.APPLICATION_ID + ".BROADCAST_STATE";
    public static String SENT_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".SENT_BROADCAST_INTENT";
    public static String FAILED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".FAILED_BROADCAST_INTENT";
    public static String DELIVERED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".DELIVERED_BROADCAST_INTENT";
    public static String REPLY_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".REPLY_BROADCAST_ACTION";
    public static String MARK_AS_READ_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".MARK_AS_READ_BROADCAST_ACTION";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(REPLY_BROADCAST_INTENT)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                String address = intent.getStringExtra(SMS.SMSMetaEntity.ADDRESS);
                String threadId = intent.getStringExtra(SMS.SMSMetaEntity.THREAD_ID);

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

                CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);

                if(reply.toString().isEmpty())
                    return;

                try {
                    int subscriptionId = SIMHandler.getDefaultSimSubscription(context);
                    SMSHandler.registerPendingMessage(context, address, reply.toString(), subscriptionId);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent receivedSmsIntent = new Intent(context, SMSSendActivity.class);
                receivedSmsIntent.putExtra(SMS.SMSMetaEntity.ADDRESS, address);
                receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Person.Builder person = new Person.Builder();
                person.setName(context.getString(R.string.notification_title_reply_you));

                PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity( context,
                        Integer.parseInt(threadId),
                        receivedSmsIntent, PendingIntent.FLAG_IMMUTABLE);
                SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
                smsMetaEntity.setThreadId(context, threadId);

                NotificationCompat.MessagingStyle messagingStyle =
                        new NotificationCompat.MessagingStyle(context.getString(R.string.notification_title_reply_you));

                String contactName = Contacts.retrieveContactName(context, smsMetaEntity.getAddress());
                contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                        smsMetaEntity.getAddress() : contactName;

                Person.Builder person1 = new Person.Builder();
                person1.setName(contactName);

                long timestamp = System.currentTimeMillis();
                NotificationCompat.Builder builder = IncomingTextSMSBroadcastReceiver
                        .getNotificationHandler(context, intent, timestamp, smsMetaEntity)
                        .setContentIntent(pendingReceivedSmsIntent);

                for(StatusBarNotification notification : notifications) {
                    if(notification.getId() == Integer.parseInt(threadId)) {
                        Bundle extras = notification.getNotification().extras;

                        CharSequence prevMessage = extras.getCharSequence(Notification.EXTRA_TEXT).toString();

                        String prevTitle = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

                        if(prevTitle.equals(context.getString(R.string.notification_title_reply_you))) {
                            messagingStyle.addMessage(prevMessage, timestamp, person.build());
                        } else {
                            messagingStyle.addMessage(
                                    new NotificationCompat.MessagingStyle.Message(prevMessage,
                                            timestamp, person1.build()));
                        }
                        break;
                    }
                }

                builder.setStyle(messagingStyle.addMessage(reply, System.currentTimeMillis(), person.build()));
                // Issue the new notification.
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                notificationManagerCompat.notify(Integer.parseInt(threadId), builder.build());
            }
        }
        else if(intent.getAction().equals(MARK_AS_READ_BROADCAST_INTENT)) {
            String threadId = intent.getStringExtra(SMS.SMSMetaEntity.THREAD_ID);
            try {
                SMSHandler.updateMarkThreadMessagesAsRead(context, threadId);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(Integer.parseInt(threadId));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        else if(intent.getAction().equals(SMS_SENT_BROADCAST_INTENT)) {
            long id = intent.getLongExtra(SMS.SMSMetaEntity.ID, -1);
            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    try {
                        SMSHandler.registerSentMessage(context, id);
                        intent.putExtra(BROADCAST_STATE, SENT_BROADCAST_INTENT);
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
                        intent.putExtra(BROADCAST_STATE, FAILED_BROADCAST_INTENT);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
            }
        }
        else if(intent.getAction().equals(SMS_DELIVERED_BROADCAST_INTENT)) {
            intent.putExtra(BROADCAST_STATE, DELIVERED_BROADCAST_INTENT);
            long id = intent.getLongExtra(SMS.SMSMetaEntity.ID, -1);
            if (getResultCode() == Activity.RESULT_OK) {
                SMSHandler.registerDeliveredMessage(context, id);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                            + getResultCode());
            }
        }

        SMSHandler.broadcastMessageStateChanged(context, intent);
    }
}
