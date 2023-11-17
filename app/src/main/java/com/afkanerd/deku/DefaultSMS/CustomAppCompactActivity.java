package com.afkanerd.deku.DefaultSMS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModel;
import androidx.preference.PreferenceManager;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.E2EE.E2EECompactActivity;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
        startServices();
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
                if(intent.getAction() != null && (
                        intent.getAction().equals(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION) ||
                intent.getAction().equals(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION))) {
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
                        ((ThreadedConversationsViewModel) viewModel).refresh(context);
                    }
                } else {
                    String messageId = intent.getStringExtra(Conversation.ID);
                    if(viewModel instanceof ConversationsViewModel && messageId != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ConversationDao conversationDao = Conversation.getDao(getApplicationContext());
                                Conversation conversation = conversationDao.getMessage(messageId);
                                conversation.setRead(true);
                                try {
                                    SharedPreferences sharedPreferences =
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    if(E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                            E2EEHandler.getKeyStoreAlias(
                                                    conversation.getAddress(), 0)) &&
                                    !sharedPreferences.getBoolean(
                                            E2EECompactActivity.INFORMED_SECURED, false)) {
                                        informSecured(true);
                                    }
                                } catch (CertificateException | KeyStoreException | IOException |
                                         NoSuchAlgorithmException | NumberParseException e) {
                                    e.printStackTrace();
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

        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_UPDATED_BROADCAST_INTENT);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_UPDATED_BROADCAST_INTENT);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter);
    }

    public void informSecured(boolean secured) { }

    private void cancelAllNotifications(int id) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(id);
    }

    public void sendTextMessage(String text, int subscriptionId,
                                ThreadedConversations threadedConversations) throws Exception {
        if(text != null) {
            final String messageId = String.valueOf(System.currentTimeMillis());
            Conversation conversation = new Conversation();
            conversation.setMessage_id(messageId);
            conversation.setText(text);
            conversation.setSubscription_id(subscriptionId);
            conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
            conversation.setDate(String.valueOf(System.currentTimeMillis()));
            conversation.setAddress(threadedConversations.getAddress());
            conversation.setStatus(Telephony.Sms.STATUS_PENDING);

            if(viewModel instanceof ConversationsViewModel) {
                ConversationsViewModel conversationsViewModel = (ConversationsViewModel) viewModel;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long id = conversationsViewModel.insert(conversation);
                            SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, null);
                            conversationsViewModel.updateThreadId(conversation.getThread_id(),
                                    messageId, id);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
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


    private void startServices() {
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        try {
            gatewayClientHandler.startServices();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            gatewayClientHandler.close();
        }

    }

}
