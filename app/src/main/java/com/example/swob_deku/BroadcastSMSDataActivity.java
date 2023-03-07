package com.example.swob_deku;

import static com.example.swob_deku.Commons.DataHelper.byteToBinary;
import static com.example.swob_deku.Commons.DataHelper.byteToChar;
import static com.example.swob_deku.Commons.DataHelper.getHexOfByte;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMS;
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
                    }

                    try {
                        Log.d(getClass().getName(), "Date image 0: " + Byte.toUnsignedInt(messageBuffer.toByteArray()[0]));
                        Log.d(getClass().getName(), "Date image 1: " + Byte.toUnsignedInt(messageBuffer.toByteArray()[1]));

                        String metaHeaderInformation = ImageHandler.IMAGE_HEADER +
                                ImageHandler.getImageMetaRIL(messageBuffer.toByteArray());

                        String strMessage = metaHeaderInformation +
                                Base64.encodeToString(messageBuffer.toByteArray(), Base64.DEFAULT);

                        Log.d(getClass().getName(), "Data image data: " + strMessage);
                        long messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);

                        String defaultRIL = Byte.toUnsignedInt(messageBuffer.toByteArray()[0]) + "0";

                        if(ImageHandler.isImageBody(messageBuffer.toByteArray())) {
                            Log.d(getClass().getName(), "Data image body found");

                            boolean canComposeImage = ImageHandler.canComposeImage(context, defaultRIL);
                            if(canComposeImage) {
                                String imageb64Data = ImageHandler.composeImage(context, defaultRIL,
                                        Byte.toUnsignedInt(messageBuffer.toByteArray()[0]));

                                Log.d(getClass().getName(), "Data image composed image: " + imageb64Data);

                                String notificationNote = "New image data!";
                                BroadcastSMSTextActivity.sendNotification(context, notificationNote, address, messageId);
                            }
                        }

                    }catch(Exception e ) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
}
