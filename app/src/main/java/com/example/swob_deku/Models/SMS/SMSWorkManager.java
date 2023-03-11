package com.example.swob_deku.Models.SMS;

import static com.example.swob_deku.Models.SMS.SMSHandler.DATA_TRANSMISSION_PORT;
import static com.example.swob_deku.SMSSendActivity.SMS_DELIVERED_INTENT;
import static com.example.swob_deku.SMSSendActivity.SMS_SENT_INTENT;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.SMSSendActivity;

import java.util.ArrayList;

public class SMSWorkManager extends Worker {
    Context context;
    String address;
    byte[] data;
    boolean hasPendingIntents;

    long messageId;

    SmsManager smsManager;

    public SMSWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

        this.address = getInputData().getString("address");
        this.data = getInputData().getByteArray("data");
        this.hasPendingIntents = getInputData().getBoolean("pending_intents", true);

        this.messageId = getInputData().getLong("message_id", -1);

        this.smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if(this.messageId != -1)
                sendDataMessage(address, data);

            else
                sendDataMessages(address, data);
        } catch(Exception e) {
            // TODO: figure out the reasons why it might fail and decide if to retry
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }

    public void handleBroadcast() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)

        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                long id = intent.getLongExtra(SMSSendActivity.ID, -1);

                if(BuildConfig.DEBUG)
                    Log.d(getClass().getName(), "Broadcast received for sent: " + id);

                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        try {
                            SMSHandler.registerSentMessage(getApplicationContext(), id);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY:
                        /**
                         * TODO:
                         *
                         * Wait (N) seconds then attempt sending the message again
                         * If new broadcast message for this message should have come, it should
                         * stop this message from proceeding.
                         * ==> After wait time, check status of message before sending again.
                         *
                         */

                        Cursor cursor = SMSHandler.fetchSMSOutboxById(getApplicationContext(), String.valueOf(id));
                        Log.d(getClass().getName(), "Broadcast should retry this message.." +
                                id + " - found: " + cursor.getCount());
                        if(cursor.moveToFirst()) {
                            SMS sms = new SMS(cursor);

                            SMSHandler.updateMessageStatus(getApplicationContext(), sms.getId(),
                                    String.valueOf(Telephony.TextBasedSmsColumns.STATUS_FAILED));

//                            SMSHandler.createWorkManagersForDataMessages(getApplicationContext(),
//                                    sms.address, Base64.decode(sms.getBody(), Base64.DEFAULT),
//                                    id);
                        }

                        cursor.close();

                        Log.d(getClass().getName(), "Broadcast retried sending message");

                        break;

                    case SmsManager.RESULT_RIL_NETWORK_ERR:
                        /**
                         * TODO
                         * This has been common with 4G devices running on
                         * Resolves itself when switched to 3G
                         */
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    default:
                        try {
                            SMSHandler.registerFailedMessage(context, id, getResultCode());
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        if(BuildConfig.DEBUG) {
                            Log.d(getClass().getName(), "Broadcast received Failed to send: "
                                    + getResultCode());
                        }
                }
            }
        };

        BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(SMSSendActivity.ID, -1);

                if (getResultCode() == Activity.RESULT_OK) {
                    SMSHandler.registerDeliveredMessage(context, id);
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(getClass().getName(), "Broadcast received Failed to deliver: "
                                + getResultCode());
                }

            }
        };

        getApplicationContext().registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_INTENT));
        getApplicationContext().registerReceiver(sentBroadcastReceiver, new IntentFilter(SMS_SENT_INTENT));
    }

    public PendingIntent[] getPendingIntents(long messageId) {
        Intent sentIntent = new Intent(SMS_SENT_INTENT);
        sentIntent.putExtra(SMSSendActivity.ID, messageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_INTENT);
        deliveredIntent.putExtra(SMSSendActivity.ID, messageId);

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), Integer.parseInt(String.valueOf(messageId)),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), Integer.parseInt(String.valueOf(messageId)),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
    }

    public void sendDataMessage(String address, byte[] data) {
        PendingIntent[] pendingIntents = getPendingIntents(messageId);
        try {
            Log.d(getClass().getName(), "Sending single new message: " + messageId);
            this.smsManager.sendDataMessage(
                    address,
                    null,
                    DATA_TRANSMISSION_PORT,
                    data,
                    pendingIntents[0],
                    pendingIntents[1]);

            SMSHandler.updateMessageStatus(context, String.valueOf(messageId),
                    String.valueOf(Telephony.TextBasedSmsColumns.STATUS_PENDING));
            Log.d(getClass().getName(), "Sent single new message: " + messageId);
        } catch(Exception e) {
            e.printStackTrace();
            try {
                SMSHandler.registerFailedMessage(context, messageId,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            } catch(Exception e1) {
                e1.printStackTrace();
                throw e1;
            }
        }
    }

    public void sendDataMessages(String address, byte[] data) throws InterruptedException {
        ArrayList<byte[]> dividedMessage = SMSHandler.structureSMSMessage(data);
        handleBroadcast();

        Log.d(getClass().getName(), "Sending multiple new message...");

//        PendingIntent[] pendingIntents = getPendingIntents(messageId);

//        long messageId = Helpers.generateRandomNumber();
//        SMSHandler.registerPendingMessage(context,
//                address,
//                Base64.encodeToString(data, Base64.DEFAULT),
//                messageId);
//
//        SMSHandler.sendTextSMS( context, address,
//                Base64.encodeToString(data, Base64.DEFAULT),
//                pendingIntents[0],
//                pendingIntents[1], messageId);

        for (int sendingMessageCounter = 0; sendingMessageCounter < dividedMessage.size(); ++sendingMessageCounter) {
            long messageId = Helpers.generateRandomNumber();

            SMSHandler.registerPendingMessage(context,
                    address,
                    Base64.encodeToString(dividedMessage.get(sendingMessageCounter), Base64.DEFAULT),
                    messageId);

            PendingIntent[] pendingIntents = getPendingIntents(messageId);
            try {

                this.smsManager.sendDataMessage(
                        address,
                        null,
                        DATA_TRANSMISSION_PORT,
                        dividedMessage.get(sendingMessageCounter),
                        pendingIntents[0],
                        pendingIntents[1]);

//                SMSHandler.registerPendingBroadcastMessage(context, messageId);
                Log.d(getClass().getName(), "Sent new message: " + messageId);
            } catch(Exception e) {
                e.printStackTrace();
                try {
                    SMSHandler.registerFailedMessage(context, messageId,
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                } catch(Exception e1) {
                    e1.printStackTrace();
                    throw e1;
                } finally {
                    Thread.sleep(1000);
                }
            }
        }

    }

}
