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
import com.example.swob_deku.Models.Security.SecurityDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;

import org.bouncycastle.operator.OperatorCreationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class BroadcastSMSDataActivity extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * Important note: either image or dump it
         */

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New data received..");

        if (intent.getAction().equals(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
                String address = "";

                for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    address = currentSMS.getDisplayOriginatingAddress();

                    try {
                        messageBuffer.write(currentSMS.getUserData());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                long messageId = -1;
                try {
                    String strMessage = messageBuffer.toString();
                    if (strMessage.contains(SecurityHelpers.FIRST_HEADER)) {
                        // TODO: register message and store the reference in a shared reference location
                        messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);
                        registerIncomingAgreement(context, address, messageBuffer.toByteArray(), 0);
                    } else if (strMessage.contains(SecurityHelpers.END_HEADER)) {
                        // TODO: search for registered message and get content from shared reference location
                        messageId = SMSHandler.registerIncomingMessage(context, address, strMessage);
                        registerIncomingAgreement(context, address, messageBuffer.toByteArray(), 1);
                    }
                    broadcastIntent(context);

                    if(checkMessagesAvailable(context, address)) {
                        String notificationNote = "New Key request";

                        BroadcastSMSTextActivity.sendNotification(context, notificationNote,
                                address, messageId);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void registerIncomingAgreement(Context context, String msisdn, byte[] keyPart, int part) throws GeneralSecurityException, IOException, OperatorCreationException {
        SecurityDH securityDH = new SecurityDH(context);
        securityDH.securelyStorePublicKeyKeyPair(context, msisdn, keyPart, part);
    }

    private void broadcastIntent(Context context) {
        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".DATA_SMS_RECEIVED_ACTION");
        context.sendBroadcast(intent);
    }

    private boolean checkMessagesAvailable(Context context, String msisdn) throws GeneralSecurityException, IOException {
        SecurityDH securityDH = new SecurityDH(context);
        return securityDH.peerAgreementPublicKeysAvailable(context, msisdn);
    }
}
