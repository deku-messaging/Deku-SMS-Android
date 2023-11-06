package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.R;

//import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
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
                String[] regIncomingOutput = new String[0];
                try {
                    regIncomingOutput = NativeSMSDB.Incoming.register_incoming_data(context, intent);

                    final String threadId = regIncomingOutput[NativeSMSDB.THREAD_ID];
                    final String messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID];
                    final String body = regIncomingOutput[NativeSMSDB.BODY];
                    final String address = regIncomingOutput[NativeSMSDB.ADDRESS];
                    final String strSubscriptionId = regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID];
                    int subscriptionId = Integer.parseInt(strSubscriptionId);

                    try {
                        String strMessage = body;
//                        if(SecurityHelpers.isKeyExchange(body)) {
//                            strMessage = registerIncomingAgreement(context, address,
//                                    Base64.decode(strMessage, Base64.DEFAULT));
//                        }

                        String notificationNote = context.getString(R.string.security_key_new_request_notification);

//                        if(smsMetaEntity.isPendingAgreement(context)) {
//                            notificationNote = context.getString(R.string.security_key_new_agreed_notification);
//
//                            strMessage = SecurityHelpers.FIRST_HEADER +
//                                    strMessage + SecurityHelpers.END_HEADER;
//
//                            NativeSMSDB.Incoming.register_incoming_text(context, intent);
//                        }

                        NotificationsHandler.sendIncomingTextMessageNotification(context, messageId);
                        broadcastIntent(context);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void broadcastIntent(Context context) {
        Intent intent = new Intent(DATA_BROADCAST_INTENT);
        context.sendBroadcast(intent);
    }
}
