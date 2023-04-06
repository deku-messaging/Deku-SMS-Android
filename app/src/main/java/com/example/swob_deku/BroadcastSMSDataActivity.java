package com.example.swob_deku;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityHelpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BroadcastSMSDataActivity extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * Important note: either image or dump it
         */

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New data received..");

        if (intent.getAction().equals(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION)) {

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
                    String address = new String();

                    for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        address = currentSMS.getDisplayOriginatingAddress();

                        try {
                            messageBuffer.write(currentSMS.getUserData());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    try {
                        String strMessage = messageBuffer.toString();
                        if(strMessage.contains(SecurityHelpers.FIRST_HEADER)){
                            // TODO: register message and store the reference in a sharedreference location
                            long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);
//                            String notificationNote = "New Key request";
//                            BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, messageId);
                        }
                        else if(strMessage.contains(SecurityHelpers.END_HEADER)){
                            // TODO: search for registered message and get content from sharedreference location
                            long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);
//                            String notificationNote = "New Key request";
//                            BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, messageId);
                        }
                        broadcastIntent(context);

                    }catch(Exception e ) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private void broadcastIntent(Context context) {
//        DATA_SMS_RECEIVED_ACTION
        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".DATA_SMS_RECEIVED_ACTION");
        context.sendBroadcast(intent);
    }
}
