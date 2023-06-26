package com.example.swob_deku.BroadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;

import java.nio.charset.StandardCharsets;

public class OutgoingDataSMSBroadcastReceiver extends BroadcastReceiver {
    private static final short DATA_TRANSMISSION_PORT = 8200;

    @Override
    public void onReceive(Context context, Intent intent) {
        Cursor cursor = null;
        try {
            long messageId = intent.getLongExtra(SMS.SMSMetaEntity.ID, -1);
            String threadId = intent.getStringExtra(SMS.SMSMetaEntity.THREAD_ID);

            SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
            smsMetaEntity.setThreadId(context, threadId);

            cursor = smsMetaEntity.fetchOutboxMessage(context, messageId);
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                smsMetaEntity.setAddress(context, sms.getAddress());

                PendingIntent[] pendingIntents = SMSHandler.getPendingIntents(context, messageId);
                sendDataSMS(context,
                        smsMetaEntity.getAddress(),
                        sms.getBody().getBytes(StandardCharsets.UTF_8),
                        pendingIntents[0], pendingIntents[1], sms.getSubscriptionId(), intent);
                cursor.close();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.send_message_error_no_text_provided),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.send_message_generic_error),
                    Toast.LENGTH_LONG).show();
        }
        finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }

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
