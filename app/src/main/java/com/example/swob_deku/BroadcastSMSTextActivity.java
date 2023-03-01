package com.example.swob_deku;

import static com.example.swob_deku.Models.SMS.SMSHandler.interpret_PDU;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.GatewayServer.GatewayServerDAO;
import com.example.swob_deku.Models.Router.Router;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BroadcastSMSTextActivity extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";
    public static final String TAG_ROUTING_URL = "swob.work.route.url,";
    public static final String TAG_WORKER_ID = "swob.work.id.";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    StringBuffer messageBuffer = new StringBuffer();
                    String address = new String();

                    for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        // TODO: Fetch address name from contact list if present
                        address = currentSMS.getDisplayOriginatingAddress();
                        String displayMessage = currentSMS.getDisplayMessageBody();
                        displayMessage = displayMessage == null ?
                                new String(currentSMS.getUserData(), StandardCharsets.UTF_8) :
                                displayMessage;
                        messageBuffer.append(displayMessage);

                        byte[] pdu = currentSMS.getPdu();

//                        if(BuildConfig.DEBUG) {
//                            try {
//                                interpret_PDU((pdu));
//                            } catch (ParseException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }

                        if (BuildConfig.DEBUG) {
                            Log.d(getClass().getName(), "PDU android studio: " + currentSMS.getServiceCenterAddress());
                        }

                        if (BuildConfig.DEBUG) {
                            Log.d(getClass().getName(), "Data received.: " + new String(currentSMS.getUserData()));
                        }
                    }

                    String message = messageBuffer.toString();
                    long messageId = SMSHandler.registerIncomingMessage(context, address, message);

                    sendNotification(context, message, address, messageId);

                    try {
                        CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
                        charsetDecoder.decode(ByteBuffer.wrap(Base64.decode(message, Base64.DEFAULT)));
                        createWorkForMessage(address, message, messageId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private void createWorkForMessage(String address, String message, long messageId) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Datastore databaseConnector = Room.databaseBuilder(this.context, Datastore.class,
                Datastore.databaseName).build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                List<GatewayServer> gatewayServerList = gatewayServerDAO.getAllList();

                for (GatewayServer gatewayServer : gatewayServerList) {
                    try {
                        OneTimeWorkRequest routeMessageWorkRequest = new OneTimeWorkRequest.Builder(Router.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(TAG_NAME)
                                .addTag(TAG_WORKER_ID + messageId)
                                .addTag(TAG_ROUTING_URL + gatewayServer.getURL())
                                .setInputData(
                                        new Data.Builder()
                                                .putString("address", address)
                                                .putString("text", message)
                                                .putString("gatewayServerUrl", gatewayServer.getURL())
                                                .build()
                                )
                                .build();

                        // String uniqueWorkName = address + message;
                        String uniqueWorkName = messageId + ":" + gatewayServer.getURL();
                        WorkManager workManager = WorkManager.getInstance(context);
                        workManager.enqueueUniqueWork(
                                uniqueWorkName,
                                ExistingWorkPolicy.KEEP,
                                routeMessageWorkRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void sendNotification(Context context, String text, String address, long messageId) {
        Intent receivedSmsIntent = new Intent(context, SMSSendActivity.class);

        receivedSmsIntent.putExtra(SMSSendActivity.ADDRESS, address);
//        receivedSmsIntent.putExtra(SMSSendActivity.THREAD_ID, threadId);

        receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity(
                context, 0, receivedSmsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contactName = Contacts.retrieveContactName(context, address);
        contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                address : contactName;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, context.getString(R.string.CHANNEL_ID))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_round_chat_bubble_24)
                .setContentTitle(contactName)
                .setContentText(text)
                .setContentIntent(pendingReceivedSmsIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(Integer.parseInt(String.valueOf(messageId)), builder.build());
    }
}