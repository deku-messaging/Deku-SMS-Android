package com.afkanerd.deku.DefaultSMS.Models;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneNumberUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationsHandler {
    @SuppressLint("MissingPermission")
    public static void sendIncomingTextMessageNotification(Context context, Conversation conversation) {
        String contactName = Contacts.retrieveContactName(context, conversation.getAddress());
        if(contactName != null)
            conversation.setAddress(contactName);


        NotificationCompat.MessagingStyle messagingStyle = getMessagingStyle(context, conversation, null);

        Intent replyIntent = getReplyIntent(context, conversation);
        PendingIntent pendingIntent = getPendingIntent(context, conversation);

        NotificationCompat.Builder builder = getNotificationBuilder(context, replyIntent,
                conversation, pendingIntent);
        builder.setStyle(messagingStyle);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(Integer.parseInt(conversation.getThread_id()), builder.build());
    }

    public static PendingIntent getPendingIntent(Context context, Conversation conversation) {
        Intent receivedIntent = getReceivedIntent(context, conversation);
        return PendingIntent.getActivity(context, Integer.parseInt(conversation.getThread_id()),
                receivedIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Person getPerson(Conversation conversation) {
        Person.Builder personBuilder = new Person.Builder()
                .setName(conversation.getAddress())
                .setKey(conversation.getThread_id());
        return personBuilder.build();
    }

    public static Intent getReplyIntent(Context context, Conversation conversation) {
        if(PhoneNumberUtils.isWellFormedSmsAddress(conversation.getAddress())) {
            Intent replyBroadcastIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);

            replyBroadcastIntent.putExtra(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_ADDRESS,
                    conversation.getAddress());

            replyBroadcastIntent.putExtra(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_THREAD_ID,
                    conversation.getThread_id());

            replyBroadcastIntent.putExtra(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_SUBSCRIPTION_ID,
                    conversation.getSubscription_id());

            replyBroadcastIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_BROADCAST_INTENT);
            return replyBroadcastIntent;
        }
        return null;
    }

    public static Intent getReceivedIntent(Context context, Conversation conversation) {
        Intent receivedSmsIntent = new Intent(context, ConversationActivity.class);
        receivedSmsIntent.putExtra(Conversation.ADDRESS, conversation.getAddress());
        receivedSmsIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());
        receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return receivedSmsIntent;
    }

    private static class MessageTrackers {
        String title;
        StringBuilder message = new StringBuilder();
        Person person;
    }

    public static NotificationCompat.MessagingStyle getMessagingStyle(Context context,
                                                                      Conversation conversation, String reply) {


        Person person = getPerson(conversation);

        Person.Builder personBuilder = new Person.Builder()
                .setName(context.getString(R.string.notification_title_reply_you))
                .setKey(context.getString(R.string.notification_title_reply_you));
        Person replyPerson = personBuilder.build();

        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        List<StatusBarNotification> notifications = notificationManager.getActiveNotifications();
        List<MessageTrackers> listMessages = new ArrayList<>();
        for(StatusBarNotification notification : notifications) {
            if (notification.getId() == Integer.parseInt(conversation.getThread_id())) {
                Bundle extras = notification.getNotification().extras;
                String prevMessage = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
                String prevTitle = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

                MessageTrackers messageTrackers = new MessageTrackers();
                messageTrackers.message.append(prevMessage);
                messageTrackers.title = prevTitle;
                messageTrackers.person = prevTitle.equals(conversation.getAddress()) ?
                        person:replyPerson;
                listMessages.add(messageTrackers);
            }
        }
        MessageTrackers messageTrackers = new MessageTrackers();
        messageTrackers.message.append(conversation.isIs_key() ?
                context.getString(R.string.notification_title_new_key) :
                reply == null ? conversation.getText() : reply);
        messageTrackers.title = reply == null ? conversation.getAddress() :
                context.getString(R.string.notification_title_reply_you);
        messageTrackers.person = reply == null ? person :replyPerson;
        listMessages.add(messageTrackers);

        for(MessageTrackers messageTracker : listMessages) {
            messagingStyle.addMessage(
                    new NotificationCompat.MessagingStyle.Message(messageTracker.message,
                            System.currentTimeMillis(),messageTracker.person));
        }

        return messagingStyle;
    }

    public static NotificationCompat.Builder
    getNotificationBuilder(Context context, Intent replyBroadcastIntent, Conversation conversation,
                           PendingIntent pendingIntent){

        String shortcutInfo = getShortcutInfo(context, conversation);

        NotificationCompat.BubbleMetadata bubbleMetadata = new NotificationCompat.BubbleMetadata
                .Builder(conversation.getAddress())
                .setDesiredHeight(400)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, context.getString(R.string.incoming_messages_channel_id))
                .setContentTitle(conversation.getAddress())
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setAllowSystemGeneratedContextualActions(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setShortcutId(shortcutInfo)
                .setBubbleMetadata(bubbleMetadata)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);


        String markAsReadLabel = context.getResources().getString(R.string.notifications_mark_as_read_label);

        Intent markAsReadIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);
        markAsReadIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());
        markAsReadIntent.putExtra(Conversation.ID, conversation.getMessage_id());
        markAsReadIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.MARK_AS_READ_BROADCAST_INTENT);

        PendingIntent markAsReadPendingIntent =
                PendingIntent.getBroadcast(context, Integer.parseInt(conversation.getThread_id()),
                        markAsReadIntent,
                        PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(null,
                markAsReadLabel, markAsReadPendingIntent)
                .build();

        builder.addAction(markAsReadAction);

        if(replyBroadcastIntent != null) {
            PendingIntent replyPendingIntent =
                    PendingIntent.getBroadcast(context, Integer.parseInt(conversation.getThread_id()),
                            replyBroadcastIntent,
                            PendingIntent.FLAG_MUTABLE);

            String replyLabel = context.getResources().getString(R.string.notifications_reply_label);
            RemoteInput remoteInput = new RemoteInput.Builder(
                    IncomingTextSMSReplyActionBroadcastReceiver.KEY_TEXT_REPLY)
                    .setLabel(replyLabel)
                    .build();

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(null,
                    replyLabel, replyPendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(replyAction);
        }

        return builder;
    }

    private static String getShortcutInfo(Context context, Conversation conversation) {
        Uri smsUrl = Uri.parse("smsto:" + conversation.getAddress());
        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUrl);
        intent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());

        Person person = getPerson(conversation);

        ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(context,
                conversation.getAddress())
                .setLongLived(true)
                .setIntent(intent)
                .setShortLabel(conversation.getAddress())
                .setPerson(person)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfoCompat);
        return shortcutInfoCompat.getId();
    }
}
