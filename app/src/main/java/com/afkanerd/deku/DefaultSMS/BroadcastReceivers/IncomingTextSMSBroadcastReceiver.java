package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.Router.Router.RouterHandler;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMS;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Notifications.NotificationsHandler;

import java.io.IOException;

public class IncomingTextSMSBroadcastReceiver extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";
    public static final String TAG_ROUTING_URL = "swob.work.route.url,";

    public static final String EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP";
    public static String SMS_SENT_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_SENT_BROADCAST_INTENT";

    public static String SMS_DELIVERED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_DELIVERED_BROADCAST_INTENT";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                final String[] regIncomingOutput;
                try {
                    regIncomingOutput = NativeSMSDB.Incoming.register_incoming_text(context, intent);
                    final String threadId = regIncomingOutput[NativeSMSDB.THREAD_ID];
                    final String messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID];
                    final String body = regIncomingOutput[NativeSMSDB.BODY];
                    final String address = regIncomingOutput[NativeSMSDB.ADDRESS];
                    final String strSubscriptionId = regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID];
                    int subscriptionId = Integer.parseInt(strSubscriptionId);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationsHandler.sendIncomingTextMessageNotification(
                                    context,
                                    body,
                                    address,
                                    Long.parseLong(messageId),
                                    subscriptionId);
                        }
                    }).start();

                    router_activities(address, body, Long.parseLong(messageId));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else if(intent.getAction().equals(SMS_SENT_BROADCAST_INTENT)) {
            long id = intent.getLongExtra(NativeSMSDB.ID, -1);
            if (getResultCode() == Activity.RESULT_OK) {
                NativeSMSDB.Outgoing.register_sent(context, id);
            } else {
                try {
                    NativeSMSDB.Outgoing.register_failed(context, id, getResultCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else if(intent.getAction().equals(SMS_DELIVERED_BROADCAST_INTENT)) {
            long id = intent.getLongExtra(NativeSMSDB.ID, -1);
            if (getResultCode() == Activity.RESULT_OK) {
                NativeSMSDB.Outgoing.register_delivered(context, id);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                            + getResultCode());
            }
        }
    }

    public void router_activities(String finalAddress, String messageFinal, long finalMessageId) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = NativeSMSDB.fetchByMessageId(context, String.valueOf(finalMessageId));
                    if(cursor != null && cursor.moveToFirst()) {
                        SMS sms = new SMS(cursor);
                        sms.setMsisdn(finalAddress);
                        sms.setText(messageFinal);

                        RouterHandler.createWorkForMessage(context, sms, finalMessageId,
                                Helpers.isBase64Encoded(messageFinal));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}