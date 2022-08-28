package com.example.swob_deku;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Models.Router;
import com.example.swob_deku.Models.SMS;
import com.example.swob_deku.Models.SMSHandler;

import java.util.concurrent.TimeUnit;

public class SMSReceiverActivity extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    for (SmsMessage currentSMS: Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        // TODO: Fetch address name from contact list if present
                        String address = currentSMS.getDisplayOriginatingAddress();

                        String message = currentSMS.getDisplayMessageBody();
                        long messageId = SMSHandler.registerIncomingMessage(context, currentSMS);
                        sendNotification(message, address, messageId);

                        try {
//                            routeMessagesToGatewayServers(context, address, message);
                            Constraints constraints = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();

                            OneTimeWorkRequest routeMessageWorkRequest = new OneTimeWorkRequest.Builder(Router.class)
                                    .setConstraints(constraints)
                                    .setBackoffCriteria(
                                            BackoffPolicy.LINEAR,
                                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                            TimeUnit.MILLISECONDS
                                    )
                                    .addTag(TAG_NAME)
                                    .addTag(address)
                                    .setInputData(
                                            new Data.Builder()
                                                    .putString("address", address)
                                                    .putString("text", message)
                                                    .build()
                                    )
                                    .build();

                            String uniqueWorkName = address + message;
                            WorkManager workManager = WorkManager.getInstance(context);
                            workManager.enqueueUniqueWork(
                                            uniqueWorkName,
                                            ExistingWorkPolicy.KEEP,
                                            routeMessageWorkRequest);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }
        }
    }

    private void sendNotification(String text, String address, long messageId) {
        Intent receivedSmsIntent = new Intent(context, SendSMSActivity.class);

        Cursor cursor = SMSHandler.fetchSMSMessageThreadIdFromMessageId(context, messageId);

        String threadId = "-1";
        if(cursor.moveToFirst()) {
            SMS sms = new SMS(cursor);
            threadId = sms.getThreadId();
        }
        receivedSmsIntent.putExtra(SendSMSActivity.ADDRESS, address);
        receivedSmsIntent.putExtra(SendSMSActivity.THREAD_ID, threadId);

        receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity(
                context, 0, receivedSmsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, context.getString(R.string.CHANNEL_ID))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_round_chat_bubble_24)
                .setContentTitle(Contacts.retrieveContactName(context, address))
                .setContentText(text)
                .setContentIntent(pendingReceivedSmsIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(Integer.parseInt(threadId), builder.build());
    }
}