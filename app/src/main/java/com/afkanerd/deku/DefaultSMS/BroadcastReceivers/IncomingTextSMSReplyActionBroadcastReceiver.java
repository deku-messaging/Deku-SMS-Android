package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
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
        if (intent.getAction() != null && intent.getAction().equals(REPLY_BROADCAST_INTENT)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                String address = intent.getStringExtra(REPLY_ADDRESS);
                String threadId = intent.getStringExtra(REPLY_THREAD_ID);

                int def_subscriptionId = SIMHandler.getDefaultSimSubscription(context);
                int subscriptionId = intent.getIntExtra(REPLY_SUBSCRIPTION_ID, def_subscriptionId);

                CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);
                if (reply == null || reply.toString().isEmpty())
                    return;

                try {
                    String messageId = String.valueOf(System.currentTimeMillis());
                    SMSDatabaseWrapper.send_text(context, messageId, address, reply.toString(),
                            subscriptionId, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Cursor cursor = NativeSMSDB.fetchByThreadId(context, threadId);
                if(cursor.moveToFirst()) {
                    Conversation conversation = Conversation.build(cursor);
                    cursor.close();

                    Person.Builder personBuilder = new Person.Builder()
                            .setName(context.getString(R.string.notification_title_reply_you));
                    Person person = personBuilder.build();

                    NotificationCompat.MessagingStyle messagingStyle =
                            NotificationsHandler.getMessagingStyle(context,
                                    context.getString(R.string.notification_title_reply_you),
                                    null, person, conversation);

                    NotificationCompat.Builder builder =
                            NotificationsHandler.getNotificationBuilder(context, person, conversation,
                            NotificationsHandler.getReplyIntent(context, conversation),
                            NotificationsHandler.getPendingIntent(context, conversation));
                    builder.setStyle(messagingStyle.addMessage(reply, System.currentTimeMillis(), person));

                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                    notificationManagerCompat.notify(Integer.parseInt(threadId), builder.build());
                }
            }
        }
        else if(intent.getAction() != null && intent.getAction().equals(MARK_AS_READ_BROADCAST_INTENT)) {
            String threadId = intent.getStringExtra(Conversation.THREAD_ID);
            String messageId = intent.getStringExtra(Conversation.ID);
            Log.d(getClass().getName(), "Got this from mark: " + messageId);
            Log.d(getClass().getName(), "Got this from thread mark: " + threadId);
            try {
                NativeSMSDB.Incoming.update_read(context, 1, threadId, messageId);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(Integer.parseInt(threadId));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
