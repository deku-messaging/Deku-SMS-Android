package com.example.swob_deku.BroadcastReceivers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Message;
import android.provider.Telephony;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationBuilderWithBuilderAccessor;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServers.GatewayServer;
import com.example.swob_deku.Models.GatewayServers.GatewayServerDAO;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.RMQ.RMQConnectionService;
import com.example.swob_deku.Models.Router.Router;
import com.example.swob_deku.Models.Router.RouterHandler;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;
import com.example.swob_deku.Models.RMQ.RMQConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IncomingTextSMSBroadcastReceiver extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";
    public static final String TAG_ROUTING_URL = "swob.work.route.url,";

    // Key for the string that's delivered in the action's intent.
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    public static final String SMS_TYPE_INCOMING = "SMS_TYPE_INCOMING";
    public static final String EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                StringBuilder messageBuffer = new StringBuilder();
                String address = "";

                // Get the Intent extras.
                Bundle bundle = intent.getExtras();

                // Get the slot and subscription keys.
//                int slot = bundle.getInt("slot", -1);

                int subscriptionId = bundle.getInt("subscription", -1);

                // Get the information about the SIM card that received the incoming message.
//                SubscriptionManager manager = SubscriptionManager.from(context);
//                SubscriptionInfo info = manager.getActiveSubscriptionInfo(subscriptionId);
                // Do something with the information about the SIM card.
//                 Log.d("TAG", "The incoming message was received on SIM " + slot + " (" + info.getDisplayName() + ")");

                for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    address = currentSMS.getDisplayOriginatingAddress();
                    String displayMessage = currentSMS.getDisplayMessageBody();
                    displayMessage = displayMessage == null ?
                            new String(currentSMS.getUserData(), StandardCharsets.UTF_8) :
                            displayMessage;
                    messageBuffer.append(displayMessage);
                }

                String message = messageBuffer.toString();
                final String finalAddress = address;

                long messageId = -1;
                try {
                    messageId = SMSHandler.registerIncomingMessage(context, finalAddress, message,
                            String.valueOf(subscriptionId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long finalMessageId = messageId;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendNotification(context, message, finalAddress, finalMessageId);
                    }
                }).start();
                final String messageFinal = message;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SmsForward smsForward = new SmsForward();
                            smsForward.MSISDN = finalAddress;
                            smsForward.text = messageFinal;

                            RouterHandler.createWorkForMessage(context, smsForward, finalMessageId,
                                    Helpers.isBase64Encoded(messageFinal));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }

    class SmsForward implements RMQConnectionService.SmsForwardInterface {
        public String type = SMS_TYPE_INCOMING;
        public String text;
        public String MSISDN;
        public String tag;

        @Override
        public void setTag(String tag) {
            this.tag = tag;
        }
    }


    public static void sendNotification(Context context, String text, final String address, long messageId) {
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

            String contactName = Contacts.retrieveContactName(context, sms.getAddress());
            contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                    smsMetaEntity.getAddress() : contactName;

            NotificationCompat.MessagingStyle messagingStyle =
                    new NotificationCompat.MessagingStyle(contactName);

            Person.Builder person = new Person.Builder();
            person.setName(contactName);
            person.setKey(sms.getAddress());

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


            NotificationCompat.Builder builder = getNotificationHandler(context, replyBroadcastIntent,
                    timestamp, smsMetaEntity)
                    .setContentIntent(pendingReceivedSmsIntent);

            builder.setContentTitle(contactName);
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
    getNotificationHandler(Context context, Intent replyBroadcastIntent, long date, SMS.SMSMetaEntity sms){

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
}