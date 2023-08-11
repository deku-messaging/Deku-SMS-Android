package com.example.swob_deku.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.google.i18n.phonenumbers.NumberParseException;

//import org.bouncycastle.operator.OperatorCreationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class IncomingDataSMSBroadcastReceiver extends BroadcastReceiver {

    public static String DATA_BROADCAST_INTENT = BuildConfig.APPLICATION_ID + ".DATA_SMS_RECEIVED_ACTION" ;

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
                String _address = "";
                String subscriptionId = "";

                for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    _address = currentSMS.getDisplayOriginatingAddress();

                    // The closest thing to subscription id is the serviceCenterAddress
                    subscriptionId = SIMHandler.getOperatorName(context, currentSMS.getServiceCenterAddress());
                    try {
                        messageBuffer.write(currentSMS.getUserData());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                long messageId = -1;
                SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
                smsMetaEntity.setAddress(context, _address);

                try {
                    String strMessage = messageBuffer.toString();
                    if(SecurityHelpers.isKeyExchange(strMessage)) {
//                        strMessage = SecurityHelpers.removeKeyWaterMark(strMessage);
                        strMessage = registerIncomingAgreement(context, smsMetaEntity.getAddress(),
                                messageBuffer.toByteArray());
                    }

                    String notificationNote = "New Key request";

                    if(smsMetaEntity.isPendingAgreement(context)) {
                        notificationNote = "Peer agreed to request";

                        strMessage = SecurityHelpers.FIRST_HEADER +
                                strMessage + SecurityHelpers.END_HEADER;

                        messageId = SMSHandler.registerIncomingMessage(context,
                                smsMetaEntity.getAddress(), strMessage, subscriptionId);

                    }

                    // TODO: change notification note
                    Log.d(getClass().getName(), "Should notify of incoming data sms...");
                    IncomingTextSMSBroadcastReceiver.sendNotification(context, notificationNote,
                            smsMetaEntity.getAddress(), messageId);
                    broadcastIntent(context);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String registerIncomingAgreement(Context context, String msisdn, byte[] keyPart) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);

//        if(securityECDH.hasSecretKey(msisdn))
//            securityECDH.removeSecretKey(msisdn);
        return securityECDH.securelyStorePeerAgreementKey(context, msisdn, keyPart);
    }


    private void broadcastIntent(Context context) {
        Intent intent = new Intent(DATA_BROADCAST_INTENT);
        context.sendBroadcast(intent);
    }
}
