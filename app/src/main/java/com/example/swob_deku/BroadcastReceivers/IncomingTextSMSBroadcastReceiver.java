package com.example.swob_deku.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.NotificationsHandler;
import com.example.swob_deku.Models.Router.RouterHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.nio.charset.StandardCharsets;

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
                int subscriptionId = bundle.getInt("subscription", -1);

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

                final long finalMessageId = messageId;
                final String messageFinal = message;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationsHandler.sendIncomingTextMessageNotification(context, message,
                                finalAddress, finalMessageId);
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Cursor cursor = SMSHandler.fetchSMSInboxById(context, String.valueOf(finalMessageId));
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
    }
}