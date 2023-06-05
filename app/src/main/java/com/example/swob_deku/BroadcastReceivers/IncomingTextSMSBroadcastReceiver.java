package com.example.swob_deku.BroadcastReceivers;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
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
import com.example.swob_deku.Models.Router.Router;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;
import com.example.swob_deku.Services.RMQConnection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IncomingTextSMSBroadcastReceiver extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";
    public static final String TAG_ROUTING_URL = "swob.work.route.url,";
    public static final String TAG_WORKER_ID = "swob.work.id.";

    // Key for the string that's delivered in the action's intent.
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    public static String SMS_SENT_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".SMS_SENT_BROADCAST_INTENT";
    public static String SMS_DELIVERED_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".SMS_DELIVERED_BROADCAST_INTENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                StringBuffer messageBuffer = new StringBuffer();
                String address = new String();

                for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    // TODO: Fetch address name from contact list if present
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
                    messageId = SMSHandler.registerIncomingMessage(context, finalAddress, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long finalMessageId = messageId;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendNotification(context, null, finalAddress, finalMessageId);
                    }
                }).start();
                final String messageFinal = message;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            createWorkForMessage(finalAddress, messageFinal, finalMessageId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }

    private void createWorkForMessage(String address, String message, long messageId) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Datastore databaseConnector = Room.databaseBuilder(this.context, Datastore.class,
                Datastore.databaseName).build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                List<GatewayServer> gatewayServerList = gatewayServerDAO.getAllList();

                for (GatewayServer gatewayServer : gatewayServerList) {
                    if(gatewayServer.getFormat().equals(GatewayServer.BASE64_FORMAT) &&
                            !Helpers.isBase64Encoded(message)) {
                        continue;
                    }

                    try {
                        OneTimeWorkRequest routeMessageWorkRequest = new OneTimeWorkRequest.Builder(Router.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(TAG_NAME)
                                .addTag(TAG_WORKER_ID + messageId)
                                .addTag(TAG_ROUTING_URL + gatewayServer.getURL())
                                .setInputData(
                                        new Data.Builder()
                                                .putString("address", address)
                                                .putString("text", message)
                                                .putString("gatewayServerUrl", gatewayServer.getURL())
                                                .build()
                                )
                                .build();

                        // String uniqueWorkName = address + message;
                        String uniqueWorkName = messageId + ":" + gatewayServer.getURL();
                        WorkManager workManager = WorkManager.getInstance(context);
                        workManager.enqueueUniqueWork(
                                uniqueWorkName,
                                ExistingWorkPolicy.KEEP,
                                routeMessageWorkRequest);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
        }).start();
    }

    public static void sendNotification(Context context, String text, final String address, long messageId) {
        Intent receivedSmsIntent = new Intent(context, SMSSendActivity.class);

        receivedSmsIntent.putExtra(SMSSendActivity.ADDRESS, address);

        Cursor cursor = SMSHandler.fetchSMSInboxById(context, String.valueOf(messageId));

        if(cursor.moveToFirst()) {
            SMS sms = new SMS(cursor);
            Cursor cursor1 = SMSHandler.fetchUnreadSMSMessagesForThreadId(context, sms.getThreadId());

            receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // TODO: check request code and make some changes
            PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity( context,
                    Integer.parseInt(sms.getThreadId()),
                    receivedSmsIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent replyBroadcastIntent = null;
            if(PhoneNumberUtils.isWellFormedSmsAddress(sms.getAddress())) {
                replyBroadcastIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);
                replyBroadcastIntent.putExtra(SMSSendActivity.ADDRESS, address);
                replyBroadcastIntent.putExtra(SMSSendActivity.THREAD_ID, sms.getThreadId());
                replyBroadcastIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.REPLY_BROADCAST_INTENT);
            }

            NotificationCompat.Builder builder = getNotificationHandler(context, cursor1,
                    null, replyBroadcastIntent, Integer.parseInt(sms.getId()), sms.getThreadId())
                    .setContentIntent(pendingReceivedSmsIntent);
            cursor1.close();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            /**
             * TODO: Using the same ID leaves notifications updated (not appended).
             * TODO: Recommendation: use groups for notifications to allow for appending them.
             */
            notificationManager.notify(Integer.parseInt(sms.getThreadId()), builder.build());
//            notificationManager.notify(Integer.parseInt(sms.id), builder.build());
        }
        cursor.close();
    }


    public static PendingIntent[] getPendingIntents(Context context, long messageId) {
        Intent sentIntent = new Intent(SMS_SENT_BROADCAST_INTENT);
        sentIntent.setPackage(context.getPackageName());
        sentIntent.putExtra(SMSSendActivity.ID, messageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_BROADCAST_INTENT);
        deliveredIntent.setPackage(context.getPackageName());
        deliveredIntent.putExtra(SMSSendActivity.ID, messageId);

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

    public static PendingIntent[] getPendingIntentsForServerRequest(Context context, long messageId, long globalMessageId) {
        Intent sentIntent = new Intent(SMS_SENT_BROADCAST_INTENT);
        sentIntent.setPackage(context.getPackageName());
        sentIntent.putExtra(SMSSendActivity.ID, messageId);
        sentIntent.putExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY, globalMessageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_BROADCAST_INTENT);
        deliveredIntent.setPackage(context.getPackageName());
        deliveredIntent.putExtra(SMSSendActivity.ID, messageId);
        deliveredIntent.putExtra(RMQConnection.MESSAGE_GLOBAL_MESSAGE_ID_KEY, globalMessageId);

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

    public static NotificationCompat.Builder
    getNotificationHandler(Context context, Cursor cursor,
                           List<NotificationCompat.MessagingStyle.Message> customMessages,
                           Intent replyBroadcastIntent, int smsId, String threadId){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, context.getString(R.string.CHANNEL_ID))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setAllowSystemGeneratedContextualActions(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        String markAsReadLabel = context.getResources().getString(R.string.notifications_mark_as_read_label);

        Intent markAsReadIntent = new Intent(context, IncomingTextSMSReplyActionBroadcastReceiver.class);
        markAsReadIntent.putExtra(SMSSendActivity.THREAD_ID, threadId);
        markAsReadIntent.setAction(IncomingTextSMSReplyActionBroadcastReceiver.MARK_AS_READ_BROADCAST_INTENT);

        PendingIntent markAsReadPendingIntent =
                PendingIntent.getBroadcast(context, smsId,
                        markAsReadIntent,
                        PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(null,
                markAsReadLabel, markAsReadPendingIntent)
                .build();
        builder.addAction(markAsReadAction);

        if(replyBroadcastIntent != null) {
            PendingIntent replyPendingIntent =
                    PendingIntent.getBroadcast(context, smsId,
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

        Person person = new Person.Builder()
                .setName(context.getString(R.string.notification_title_reply_you))
                .build();
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person);

        List<NotificationCompat.MessagingStyle.Message> unreadMessages = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                SMS unreadSMS = new SMS(cursor);
                String contactName = Contacts.retrieveContactName(context, unreadSMS.getAddress());
                contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                        unreadSMS.getAddress() : contactName;
                SpannableStringBuilder spannable = new SpannableStringBuilder(contactName);

                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                StyleSpan ItalicSpan = new StyleSpan(Typeface.ITALIC);

                spannable.setSpan(boldSpan, 0, contactName.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                if(unreadSMS.getBody().contains(ImageHandler.IMAGE_HEADER)) {
                    String message = context.getString(R.string.notification_title_new_photo);
                    SpannableStringBuilder spannableMessage = new SpannableStringBuilder(message);
                    spannableMessage.setSpan(ItalicSpan, 0, message.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    unreadMessages.add(new NotificationCompat.MessagingStyle.Message(
                            spannableMessage,
                            Long.parseLong(unreadSMS.getDate()),
                            spannable));
                } else if(SecurityHelpers.isKeyExchange(unreadSMS.getBody())){
                    String message = context.getString(R.string.notification_title_new_key);
                    SpannableStringBuilder spannableMessage = new SpannableStringBuilder(message);
                    spannableMessage.setSpan(ItalicSpan, 0, message.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    unreadMessages.add(new NotificationCompat.MessagingStyle.Message(
                            spannableMessage,
                            Long.parseLong(unreadSMS.getDate()),
                            spannable));
                } else {
                    unreadMessages.add(new NotificationCompat.MessagingStyle.Message(
                            unreadSMS.getBody() + "\n",
                            Long.parseLong(unreadSMS.getDate()), spannable));
                }
            } while(cursor.moveToNext());
        }

        for(NotificationCompat.MessagingStyle.Message message : unreadMessages) {
            messagingStyle.addMessage(message);
        }
        if(customMessages != null) {
            for(NotificationCompat.MessagingStyle.Message message : customMessages) {
                messagingStyle.addMessage(message);
            }
        }
        return builder.setStyle(messagingStyle);
    }
}