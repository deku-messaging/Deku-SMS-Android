package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.Router.Router.RouterItem;
import com.afkanerd.deku.Router.Router.RouterHandler;

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

    /*
    - address received might be different from how address is saved.
    - how it received is the trusted one, but won't match that which has been saved.
    - when message gets stored it's associated to the thread - so matching is done by android
    - without country code, can't know where message is coming from. Therefore best assumption is
    - service providers do send in country code.
    - How is matched to users stored without country code?
     */


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                try {
                    final String[] regIncomingOutput = NativeSMSDB.Incoming.register_incoming_text(context, intent);
                    if(regIncomingOutput != null) {
                        final String messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID];

                        NotificationsHandler.sendIncomingTextMessageNotification(context, messageId);
                        router_activities(messageId);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else if(intent.getAction().equals(SMS_SENT_BROADCAST_INTENT)) {
            String id = intent.getStringExtra(NativeSMSDB.ID);
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
            String id = intent.getStringExtra(NativeSMSDB.ID);
            if (getResultCode() == Activity.RESULT_OK) {
                NativeSMSDB.Outgoing.register_delivered(context, id);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                            + getResultCode());
            }
        }
    }

    public void router_activities(String messageId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
                    if(cursor.moveToFirst()) {
                        RouterItem routerItem = new RouterItem(cursor);
                        cursor.close();
                        RouterHandler.route(context, routerItem);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}