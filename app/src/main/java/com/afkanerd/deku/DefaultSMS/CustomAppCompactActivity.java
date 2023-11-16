package com.afkanerd.deku.DefaultSMS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

public class CustomAppCompactActivity extends DualSIMConversationActivity {
    BroadcastReceiver generateUpdateEventsBroadcastReceiver;
    BroadcastReceiver smsDeliverActionBroadcastReceiver;
    BroadcastReceiver smsSentBroadcastIntent;
    BroadcastReceiver smsDeliveredBroadcastIntent;
    BroadcastReceiver dataSentBroadcastIntent;
    BroadcastReceiver dataDeliveredBroadcastIntent;

    public static final String TAG_NAME = "NATIVE_CONVERSATION_TAG";
    public static final String UNIQUE_WORK_NAME = "NATIVE_CONVERSATION_TAG_UNIQUE_WORK_NAME";
    public static final String LOAD_NATIVES = "LOAD_NATIVES";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        loadConversationsNativesBg();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                SharedPreferences sharedPreferences =
//                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                if(sharedPreferences.getBoolean(LOAD_NATIVES, true)) {
//                    loadConversationsNativesBg();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(),
//                                    getString(R.string.threading_conversations_natives_loaded), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

    private void loadConversationsNativesBg() {
//        Constraints constraints = new Constraints.Builder()
//                .build();
//        OneTimeWorkRequest routeMessageWorkRequest =
//                new OneTimeWorkRequest.Builder(ConversationWorkManager.class)
//                .setConstraints(constraints)
//                .setBackoffCriteria(
//                        BackoffPolicy.LINEAR,
//                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
//                        TimeUnit.MILLISECONDS
//                )
//                .addTag(TAG_NAME)
//                .build();
//
//        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
//        workManager.enqueueUniqueWork(
//                UNIQUE_WORK_NAME,
//                ExistingWorkPolicy.KEEP,
//                routeMessageWorkRequest);
    }
    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    ViewModel viewModel;
    public void setViewModel(ViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void configureBroadcastListeners() {

        generateUpdateEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() == null)
                    return;

                if(intent.getAction().equals(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION) ||
                intent.getAction().equals(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION)) {
                    String messageId = intent.getStringExtra(Conversation.ID);
                    if(viewModel instanceof ConversationsViewModel) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ConversationDao conversationDao = Conversation.getDao(getApplicationContext());
                                Conversation conversation = conversationDao.getMessage(messageId);
                                conversation.setRead(true);
                                ((ConversationsViewModel) viewModel).update(conversation);
                            }
                        }).start();
                    } else if(viewModel instanceof ThreadedConversationsViewModel) {
                        Log.d(getLocalClassName(), "yes getting the intent");
                        ((ThreadedConversationsViewModel) viewModel).refresh(context);
                    }
                } else {
                    String messageId = intent.getStringExtra(Conversation.ID);
                    if(viewModel instanceof ConversationsViewModel) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ConversationDao conversationDao = Conversation.getDao(getApplicationContext());
                                Conversation conversation = conversationDao.getMessage(messageId);
                                if (getResultCode() == Activity.RESULT_OK) {
                                    if(intent.getAction().equals(
                                            IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT)
                                    || intent.getAction().equals(
                                            IncomingDataSMSBroadcastReceiver.DATA_DELIVERED_BROADCAST_INTENT)) {
                                        conversation.setStatus(
                                                Telephony.TextBasedSmsColumns.STATUS_COMPLETE);
                                    }
                                    else if(intent.getAction().equals(
                                            IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT)
                                    || intent.getAction().equals(
                                            IncomingDataSMSBroadcastReceiver.DATA_SENT_BROADCAST_INTENT)) {
                                        conversation.setStatus(
                                                Telephony.TextBasedSmsColumns.STATUS_NONE);
                                    }
                                }
                                else {
                                    conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                                    conversation.setError_code(getResultCode());
                                }
                                ((ConversationsViewModel) viewModel).update(conversation);
                            }
                        }).start();
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION);

        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT);
        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_SENT_BROADCAST_INTENT);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_DELIVERED_BROADCAST_INTENT);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter);
    }

    private void cancelAllNotifications(int id) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(generateUpdateEventsBroadcastReceiver != null)
            unregisterReceiver(generateUpdateEventsBroadcastReceiver);

        if(smsDeliverActionBroadcastReceiver != null)
            unregisterReceiver(smsDeliverActionBroadcastReceiver);

        if(smsSentBroadcastIntent != null)
            unregisterReceiver(smsSentBroadcastIntent);

        if(smsDeliveredBroadcastIntent != null)
            unregisterReceiver(smsDeliveredBroadcastIntent);

        if(dataSentBroadcastIntent != null)
            unregisterReceiver(dataSentBroadcastIntent);

        if(dataDeliveredBroadcastIntent != null)
            unregisterReceiver(dataDeliveredBroadcastIntent);
    }

    public void cancelNotifications(String threadId) {
        if (!threadId.isEmpty()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                    getApplicationContext());
            notificationManager.cancel(Integer.parseInt(threadId));
        }
    }

}
