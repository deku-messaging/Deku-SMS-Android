package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.KEY_TEXT_REPLY;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.DefaultSMS.SMSSendActivity;

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
                NotificationCompat.Builder builder = NotificationsHandler
                        .getNotificationHandler(context, intent, timestamp, smsMetaEntity,
                                smsMetaEntity.getAddress())
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
        else if(intent.getAction().equals(SMSHandler.SMS_SENT_BROADCAST_INTENT)) {
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
        else if(intent.getAction().equals(SMSHandler.SMS_DELIVERED_BROADCAST_INTENT)) {
            long id = intent.getLongExtra(SMS.SMSMetaEntity.ID, -1);
            if (getResultCode() == Activity.RESULT_OK) {
                SMSHandler.registerDeliveredMessage(context, id);
                intent.putExtra(BROADCAST_STATE, DELIVERED_BROADCAST_INTENT);
                Log.d(getClass().getName(), "Sending Delivered broadcast to all out there who listen");
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                            + getResultCode());
            }
        }

        SMSHandler.broadcastMessageStateChanged(context, intent);
    }
}
