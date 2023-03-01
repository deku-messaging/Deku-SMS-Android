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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
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

    public SMSWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

        this.address = getInputData().getString("address");
        this.data = getInputData().getByteArray("data");
        this.hasPendingIntents = getInputData().getBoolean("pending_intents", true);

    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            sendDataMessages(address, data, hasPendingIntents);
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
                        // TODO: handle the bits and pieces that fail here -
                        Log.d(getClass().getName(), "Broadcast should retry this message.." + id);
                        break;

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
                            Log.d(getClass().getName(), "Broadcast Failed to send: " + getResultCode());
                        }
                }
//                context.unregisterReceiver(this);
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
                        Log.d(getClass().getName(), "Failed to deliver: " + getResultCode());
                }

//                context.unregisterReceiver(this);
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

    public void sendDataMessages(String address, byte[] data, boolean hasPendingIntents) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();


        ArrayList<byte[]> dividedMessage = SMSHandler.structureSMSMessage(data);
        handleBroadcast();
        for (int sendingMessageCounter = 0; sendingMessageCounter < dividedMessage.size(); ++sendingMessageCounter) {
            long messageId = Helpers.generateRandomNumber();

            SMSHandler.registerPendingMessage(context,
                    address,
                    Base64.encodeToString(dividedMessage.get(sendingMessageCounter), Base64.NO_PADDING),
                    messageId);

            PendingIntent[] pendingIntents = getPendingIntents(messageId);
            try {
                Log.d(getClass().getName(), "Sending new message: " + messageId);
                smsManager.sendDataMessage(
                        address,
                        null,
                        DATA_TRANSMISSION_PORT,
                        dividedMessage.get(sendingMessageCounter),
                        pendingIntents[0],
                        pendingIntents[1]);

                SMSHandler.registerPendingBroadcastMessage(context, messageId);
                Log.d(getClass().getName(), "Sent new message: " + messageId);
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

    }
}
