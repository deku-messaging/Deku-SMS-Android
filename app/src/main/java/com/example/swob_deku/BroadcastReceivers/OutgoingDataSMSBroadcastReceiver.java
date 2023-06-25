package com.example.swob_deku.BroadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMSHandler;

public class OutgoingDataSMSBroadcastReceiver extends BroadcastReceiver {
    private static final short DATA_TRANSMISSION_PORT = 8200;

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    private void sendDataSMS(Context context, String destinationAddress, byte[] data,
                             PendingIntent sentIntent, PendingIntent deliveryIntent,
                             Integer subscriptionId, Intent intent) throws Exception {
        if (data == null)
            throw new Exception(getClass().getName() + ": Cannot send null data");

        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
        try {
            smsManager.sendDataMessage(
                    destinationAddress,
                    null,
                    DATA_TRANSMISSION_PORT,
                    data,
                    sentIntent,
                    deliveryIntent);
        } catch (Exception e) {
            throw new Exception(e);
        }
        SMSHandler.broadcastMessageStateChanged(context, intent);
    }
}
