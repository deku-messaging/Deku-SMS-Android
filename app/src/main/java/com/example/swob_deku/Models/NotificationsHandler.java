package com.example.swob_deku.Models;

import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.EXTRA_TIMESTAMP;
import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.KEY_TEXT_REPLY;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSReplyActionBroadcastReceiver;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;

public class NotificationsHandler {

    @SuppressLint("MissingPermission")
    public static void sendIncomingTextMessageNotification(Context context, String text, final String address, long messageId) {
        Intent receivedSmsIntent = new Intent(context, SMSSendActivity.class);

        Cursor cursor = SMSHandler.fetchSMSInboxById(context, String.valueOf(messageId));
        if(cursor.moveToFirst()) {
            SMS sms = new SMS(cursor);
            SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
            smsMetaEntity.setThreadId(context, sms.getThreadId());

//            Cursor cursor1 = smsMetaEntity.fetchUnreadMessages(context);
            receivedSmsIntent.putExtra(SMS.SMSMetaEntity.ADDRESS, sms.getAddress());
            receivedSmsIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, sms.getThreadId());

            receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity( context,
                    Integer.parseInt(sms.getThreadId()),
                    receivedSmsIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Intent replyBroadcastIntent = null;
            if(PhoneNumberUtils.isWellFormedSmsAddress(sms.getAddress())) {
                replyBroadcastIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);
                replyBroadcastIntent.putExtra(SMS.SMSMetaEntity.ADDRESS, address);
                replyBroadcastIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, sms.getThreadId());
                replyBroadcastIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_BROADCAST_INTENT);
            }

            if(text.contains(ImageHandler.IMAGE_HEADER)) {
                text = context.getString(R.string.notification_title_new_photo);
            } else if(SecurityHelpers.isKeyExchange(text)) {
                text = context.getString(R.string.notification_title_new_key);
            }

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

            long timestamp = System.currentTimeMillis();

            String contactName = Contacts.retrieveContactName(context, smsMetaEntity.getAddress());
            contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                    smsMetaEntity.getAddress() : contactName;

            NotificationCompat.MessagingStyle messagingStyle =
                    new NotificationCompat.MessagingStyle(contactName);

            Bitmap bitmap = Contacts.getContactBitmapPhoto(context, smsMetaEntity.getAddress());
            Person.Builder person = new Person.Builder();

            person.setName(contactName)
                    .setKey(smsMetaEntity.getAddress());
            if(bitmap != null)
                person.setIcon(IconCompat.createWithBitmap(bitmap));

            for(StatusBarNotification notification : notifications) {
                if (notification.getId() == Integer.parseInt(sms.getThreadId())) {
                    Bundle extras = notification.getNotification().extras;
                    String prevMessage = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
                    String prevTitle = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

                    if(!prevTitle.equals(context.getString(R.string.notification_title_reply_you))) {
//                        text = prevMessage + "\n" + text;
                        messagingStyle.addMessage(prevMessage, timestamp, person.build());
                    } else {
                        Person.Builder person1 = new Person.Builder();
                        person1.setName(context.getString(R.string.notification_title_reply_you));

                        messagingStyle.addMessage(new NotificationCompat.MessagingStyle.Message(
                                prevMessage, timestamp, person1.build()));
                    }
                    break;
                }
            }

            String shortCutId = _buildShortcut(context, messageId, text, person.build());
            NotificationCompat.Builder builder = getNotificationHandler(context, replyBroadcastIntent,
                    timestamp, smsMetaEntity, shortCutId)
                    .setContentIntent(pendingReceivedSmsIntent);

            builder.setContentTitle(contactName)
                    .setContentText(text);

            messagingStyle.addMessage(text, timestamp, person.build());
            builder.setStyle(messagingStyle);

            Bundle extras = builder.getExtras();
            extras.putLong(EXTRA_TIMESTAMP, timestamp);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(Integer.parseInt(sms.getThreadId()), builder.build());
        }
        cursor.close();
    }

    public static NotificationCompat.Builder
    getNotificationHandler(Context context, Intent replyBroadcastIntent, long date, SMS.SMSMetaEntity sms,
                           String shortcutId){

        NotificationCompat.BubbleMetadata bubbleMetadata = new NotificationCompat.BubbleMetadata
                .Builder(sms.getAddress())
                .setDesiredHeight(600)
                .setSuppressNotification(true)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, context.getString(R.string.incoming_messages_channel_id))
                .setWhen(date)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setAllowSystemGeneratedContextualActions(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setShortcutId(shortcutId)
                .setBubbleMetadata(bubbleMetadata)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);


        String markAsReadLabel = context.getResources().getString(R.string.notifications_mark_as_read_label);

        Intent markAsReadIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);
        markAsReadIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, sms.getThreadId());
        markAsReadIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.MARK_AS_READ_BROADCAST_INTENT);

        PendingIntent markAsReadPendingIntent =
                PendingIntent.getBroadcast(context, Integer.parseInt(sms.getThreadId()),
                        markAsReadIntent,
                        PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(null,
                markAsReadLabel, markAsReadPendingIntent)
                .build();

        builder.addAction(markAsReadAction);

        if(replyBroadcastIntent != null) {
            PendingIntent replyPendingIntent =
                    PendingIntent.getBroadcast(context, Integer.parseInt(sms.getThreadId()),
                            replyBroadcastIntent,
                            PendingIntent.FLAG_MUTABLE);

            String replyLabel = context.getResources().getString(R.string.notifications_reply_label);
            RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
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

    private static String _buildShortcut(Context context, long messageId, String text, Person person) {
        Cursor cursor = SMSHandler.fetchSMSInboxById(context, String.valueOf(messageId));
        if(cursor.moveToFirst()) {
            SMS sms = new SMS(cursor);
            SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
            smsMetaEntity.setThreadId(context, sms.getThreadId());

            String contactName = Contacts.retrieveContactName(context, smsMetaEntity.getAddress());
            contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                    smsMetaEntity.getAddress() : contactName;

            Uri smsUrl = Uri.parse("smsto:" + smsMetaEntity.getAddress());
            Intent intent = new Intent(Intent.ACTION_SENDTO, smsUrl);
            intent.putExtra(SMS.SMSMetaEntity.THREAD_ID, smsMetaEntity.getAddress())
                    .putExtra(SMS.SMSMetaEntity.SHARED_SMS_BODY, text);

            ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(context,
                    smsMetaEntity.getAddress())
                    .setLongLived(true)
                    .setIntent(intent)
                    .setShortLabel(contactName)
                    .setPerson(person)
                    .build();

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfoCompat);
            return shortcutInfoCompat.getId();
        }

        return null;
    }
}
