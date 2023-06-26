package com.example.swob_deku.BroadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;

import java.util.ArrayList;
import java.util.Arrays;

public class OutgoingTextSMSBroadcastReceiver extends BroadcastReceiver {
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
                sendTextSMS(context,
                        smsMetaEntity.getAddress(),
                        sms.getBody(),
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
    private void sendTextSMS(Context context, String destinationAddress, String text,
                               PendingIntent sentIntent, PendingIntent deliveryIntent,
                               Integer subscriptionId, Intent intent) throws Exception {

        if (text == null || text.isEmpty())
            throw new Exception(getClass().getName() + ": Cannot send null text");

        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);

        try {
            ArrayList<String> dividedMessage = smsManager.divideMessage(text);
            if (dividedMessage.size() < 2)
                smsManager.sendTextMessage(destinationAddress, null, text, sentIntent, deliveryIntent);
            else {
                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();

                for (int i = 0; i < dividedMessage.size() - 1; i++) {
                    sentPendingIntents.add(null);
                    deliveredPendingIntents.add(null);
                }

                sentPendingIntents.add(sentIntent);
                deliveredPendingIntents.add(deliveryIntent);

                smsManager.sendMultipartTextMessage(destinationAddress, null,
                        dividedMessage, sentPendingIntents, deliveredPendingIntents);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }

        SMSHandler.broadcastMessageStateChanged(context, intent);
    }
}
