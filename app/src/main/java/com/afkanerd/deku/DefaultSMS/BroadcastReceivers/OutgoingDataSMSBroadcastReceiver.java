package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSMetaEntity;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.afkanerd.deku.DefaultSMS.R;

public class OutgoingDataSMSBroadcastReceiver extends BroadcastReceiver {
    private static final short DATA_TRANSMISSION_PORT = 8200;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(SMSHandler.SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST) ||
                intent.getAction().equals(SMSHandler.SMS_NEW_DATA_REGISTERED_PENDING_BROADCAST)) {
            Cursor cursor = null;
            try {
                long messageId = intent.getLongExtra(SMSMetaEntity.ID, -1);
                String threadId = intent.getStringExtra(SMSMetaEntity.THREAD_ID);

                SMSMetaEntity smsMetaEntity = new SMSMetaEntity();
                smsMetaEntity.setThreadId(context, threadId);

                cursor = smsMetaEntity.fetchOutboxMessage(context, messageId);
                if(cursor.moveToFirst()) {
                    SMS sms = new SMS(cursor);
                    smsMetaEntity.setAddress(context, sms.getAddress());

                    PendingIntent[] pendingIntents = SMSHandler.getPendingIntents(context, messageId);
                    String body = sms.getBody();

                    if(intent.getAction().equals(SMSHandler.SMS_NEW_KEY_REGISTERED_PENDING_BROADCAST))
                        body = SecurityHelpers.removeKeyWaterMark(body);

                    sendDataSMS(context,
                            smsMetaEntity.getAddress(),
                            Base64.decode(body, Base64.DEFAULT),
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
    }

    private void sendDataSMS(Context context, String destinationAddress, byte[] data,
                             PendingIntent sentIntent, PendingIntent deliveryIntent,
                             Integer subscriptionId, Intent intent) throws Exception {
        if (data == null)
            throw new Exception(getClass().getName() + ": Cannot send null data");
        Log.d(getClass().getName(), "Sending data of size: " + data.length);

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
