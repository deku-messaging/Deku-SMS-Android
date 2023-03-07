package com.example.swob_deku;

import static com.example.swob_deku.Commons.DataHelper.byteToBinary;
import static com.example.swob_deku.Commons.DataHelper.byteToChar;
import static com.example.swob_deku.Commons.DataHelper.getHexOfByte;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

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

//                        try {
//                            byte[] pdu = currentSMS.getPdu();
//                            SMSHandler.interpret_PDU(pdu);
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }
                    }

                    try {
                        String strMessage = Base64.encodeToString(messageBuffer.toByteArray(), Base64.NO_PADDING);
                        long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);

                        if(ImageHandler.isImageBody(messageBuffer.toByteArray())) {
                            // TODO: Extract details and find others
                            byte[] imageMeta = ImageHandler.extractMeta(messageBuffer.toByteArray());
                            String[] imageData = ImageHandler.fetchImage(context, imageMeta, messageId);

                            if(imageData == null)
                                return;

                            ImageHandler.rebuildImage(context, imageData);
                            // THis is image
                            String notificationNote = "New image data!";
                            BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, Long.parseLong(imageData[0]));
                        }

                    }catch(Exception e ) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
}
