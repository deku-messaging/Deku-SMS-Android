package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Notifications.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSMetaEntity;
import com.afkanerd.deku.DefaultSMS.R;

public class IncomingTextSMSReplyActionBroadcastReceiver extends BroadcastReceiver {
    public static String BROADCAST_STATE = BuildConfig.APPLICATION_ID + ".BROADCAST_STATE";
    public static String SENT_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".SENT_BROADCAST_INTENT";
    public static String FAILED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".FAILED_BROADCAST_INTENT";
    public static String DELIVERED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".DELIVERED_BROADCAST_INTENT";
    public static String REPLY_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".REPLY_BROADCAST_ACTION";
    public static String MARK_AS_READ_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".MARK_AS_READ_BROADCAST_ACTION";

    public static String REPLY_ADDRESS = "REPLY_ADDRESS";
    public static String REPLY_THREAD_ID = "REPLY_THREAD_ID";
    public static String REPLY_SUBSCRIPTION_ID = "REPLY_SUBSCRIPTION_ID";

    // Key for the string that's delivered in the action's intent.
    public static final String KEY_TEXT_REPLY = "KEY_TEXT_REPLY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(REPLY_BROADCAST_INTENT)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                String address = intent.getStringExtra(REPLY_ADDRESS);
                String threadId = intent.getStringExtra(REPLY_THREAD_ID);

                int def_subscriptionId = SIMHandler.getDefaultSimSubscription(context);
                int subscriptionId = intent.getIntExtra(REPLY_SUBSCRIPTION_ID, def_subscriptionId);

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

                CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);

                if (reply == null || reply.toString().isEmpty())
                    return;

                try {
                    NativeSMSDB.Outgoing.send_text(context, address, reply.toString(),
                            subscriptionId, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent receivedSmsIntent = new Intent(context, ConversationActivity.class);
                receivedSmsIntent.putExtra(SMSMetaEntity.ADDRESS, address);
                receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Person.Builder person = new Person.Builder();
                person.setName(context.getString(R.string.notification_title_reply_you));

                PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity(context,
                        Integer.parseInt(threadId),
                        receivedSmsIntent, PendingIntent.FLAG_IMMUTABLE);
                SMSMetaEntity smsMetaEntity = new SMSMetaEntity();
                smsMetaEntity.setThreadId(context, threadId);

                NotificationCompat.MessagingStyle messagingStyle =
                        new NotificationCompat.MessagingStyle(context.getString(R.string.notification_title_reply_you));

                String contactName = Contacts.retrieveContactName(context, smsMetaEntity.getAddress());
                contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                        smsMetaEntity.getAddress() : contactName;

                Person.Builder person1 = new Person.Builder();
                person1.setName(contactName);

                long timestamp = System.currentTimeMillis();
                NotificationCompat.Builder builder = NotificationsHandler
                        .getNotificationHandler(context, intent, timestamp, smsMetaEntity,
                                smsMetaEntity.getAddress())
                        .setContentIntent(pendingReceivedSmsIntent);

                for (StatusBarNotification notification : notifications) {
                    if (notification.getId() == Integer.parseInt(threadId)) {
                        Bundle extras = notification.getNotification().extras;

                        CharSequence prevMessage = extras.getCharSequence(Notification.EXTRA_TEXT).toString();

                        String prevTitle = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

                        if (prevTitle.equals(context.getString(R.string.notification_title_reply_you))) {
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
            String threadId = intent.getStringExtra(SMSMetaEntity.THREAD_ID);
            try {
                SMSHandler.updateMarkThreadMessagesAsRead(context, threadId);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(Integer.parseInt(threadId));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
