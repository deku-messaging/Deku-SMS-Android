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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                    ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
                    String address = new String();

                    for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        address = currentSMS.getDisplayOriginatingAddress();

                        try {
                            messageBuffer.write(currentSMS.getUserData());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            byte[] pdu = currentSMS.getPdu();
                            SMSHandler.interpret_PDU(pdu);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if(BuildConfig.DEBUG) {
                        Log.d(getClass().getName(), "Message Address: " + address);
                        Log.d(getClass().getName(), "Message bytes: " + messageBuffer);
                    }
//                    String b64Message = new String(Base64.encode(messageBuffer, Base64.DEFAULT), StandardCharsets.UTF_8);
                    byte[] extractedMeta = extractMessageMeta(messageBuffer.toByteArray());
                    if(extractedMeta != null)
                        for(int i=0;i<extractedMeta.length;++i) {
                            Log.d(getClass().getName(), "PDU Extracted meta: " + i + "-> " + extractedMeta[i]);
                        }
                    else
                        Log.d(getClass().getName(), "PDU extracted was null");

                    Log.d(getClass().getName(), "Data Header raw: " + Byte.toUnsignedInt(messageBuffer.toByteArray()[0]));

                    String strMessage = Base64.encodeToString(messageBuffer.toByteArray(), Base64.NO_PADDING);
                    Log.d(getClass().getName(), "Data Header storing: " + strMessage);

                    long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);

                    // TODO: silence for now
                    String notificationNote = "New image data!";
                    BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, messageId);
                    break;
            }
        }
    }

    public byte[] extractMessageMeta(byte[] data) {
        if(data.length < 2)
            return null;

        /**
         * 0 = Reference ID
         * 1 = Message ID
         * 2 = Total number of messages
         */
        if(data[1] == (byte) 0)
            return new byte[]{data[0], data[1], data[2]};
        return new byte[]{data[0], data[1]};
    }
}
