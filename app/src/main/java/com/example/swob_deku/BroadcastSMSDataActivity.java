package com.example.swob_deku;

import static com.example.swob_deku.Commons.DataHelper.byteToBinary;
import static com.example.swob_deku.Commons.DataHelper.byteToChar;
import static com.example.swob_deku.Commons.DataHelper.getHexOfByte;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.Models.SMS.SMSHandler;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

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

                        byte[] pdu = currentSMS.getPdu();
                        messageBuffer = currentSMS.getUserData();
//                        try {
//                            SMSHandler.interpret_PDU(pdu);
//                        } catch (ParseException e) {
//                            throw new RuntimeException(e);
//                        }
                    }

                    if(BuildConfig.DEBUG) {
                        Log.d(getClass().getName(), "Message Address: " + address);
                        Log.d(getClass().getName(), "Message bytes: " + messageBuffer);
                    }

//                    String b64Message = new String(Base64.encode(messageBuffer, Base64.DEFAULT), StandardCharsets.UTF_8);
                    String strMessage = new String(messageBuffer, StandardCharsets.UTF_8);
                    Log.d(getClass().getName(), "PDU data incoming: " + strMessage);
                    long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);

                    String notificationNote = "New image data!";
                    BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, messageId);
                    break;
            }
        }
    }
}
