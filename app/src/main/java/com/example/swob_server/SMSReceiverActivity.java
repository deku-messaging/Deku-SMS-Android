package com.example.swob_server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.swob_server.Models.SMSHandler;
import com.example.swob_server.R;

public class SMSReceiverActivity extends BroadcastReceiver {
    private Bundle bundle;
    private SmsMessage currentSMS;
    private String message;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

            bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdu_Objects = (Object[]) bundle.get("pdus");
                if (pdu_Objects != null) {
                    for (Object aObject : pdu_Objects) {
                        currentSMS = getIncomingMessage(aObject, bundle);

                        // TODO: Fetch address name from contact list if present
                        String address = currentSMS.getDisplayOriginatingAddress();

                        message = currentSMS.getDisplayMessageBody();
                        SMSHandler.registerIncomingMessage(context, currentSMS);
                        sendNotification("123456", context, message, address);
                    }
                }
            }
        } // bundle null
    }

    private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
        SmsMessage currentSMS = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                SmsMessage.createFromPdu((byte[]) aObject, bundle.getString("format")):
                SmsMessage.createFromPdu((byte[]) aObject);

        return currentSMS;
    }


    private void sendNotification(String channel_id, Context context, String text, String address) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel_id)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New SMS from " + address)
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(8888, builder.build());
    }

}