package com.example.swob_deku;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.swob_deku.Models.SMS.SMSHandler;

import java.nio.charset.StandardCharsets;

public class BroadcastSMSDataActivity extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New data received..");

        if (intent.getAction().equals(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    byte[] messageBuffer = new byte[]{};
                    StringBuffer messageStringBuffer = new StringBuffer();
                    String address = new String();

                    for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        address = currentSMS.getDisplayOriginatingAddress();
                        messageBuffer = currentSMS.getPdu();
                        messageStringBuffer.append(new String(currentSMS.getUserData(), StandardCharsets.UTF_8));
                    }

                    if(BuildConfig.DEBUG) {
                        Log.d(getClass().getName(), "Message Address: " + address);
                        Log.d(getClass().getName(), "Message bytes: " + new String(messageBuffer, StandardCharsets.UTF_8));
                        Log.d(getClass().getName(), "Message string: " + messageStringBuffer);
                    }

                    String stringMessage = messageStringBuffer.toString();
                    long messageId = SMSHandler.registerIncomingMessage(context, address, stringMessage);

                    BroadcastSMSTextActivity.sendNotification(context, stringMessage, address, messageId);
                    break;
            }
        }
    }
}
