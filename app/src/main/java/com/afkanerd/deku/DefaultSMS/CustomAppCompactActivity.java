package com.afkanerd.deku.DefaultSMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomAppCompactActivity extends DualSIMConversationActivity {
    BroadcastReceiver generateUpdateEventsBroadcastReceiver;
    BroadcastReceiver smsDeliverActionBroadcastReceiver;
    BroadcastReceiver smsSentBroadcastIntent;
    BroadcastReceiver smsDeliveredBroadcastIntent;
    BroadcastReceiver dataSentBroadcastIntent;
    BroadcastReceiver dataDeliveredBroadcastIntent;

    protected static final String TAG_NAME = "NATIVE_CONVERSATION_TAG";
    protected static final String UNIQUE_WORK_NAME = "NATIVE_CONVERSATION_TAG_UNIQUE_WORK_NAME";

    protected final static String DRAFT_PRESENT_BROADCAST = "DRAFT_PRESENT_BROADCAST";

    protected ConversationsViewModel conversationsViewModel;

    protected ThreadedConversationsViewModel threadedConversationsViewModel;

    protected ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }
    }
    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    protected void configureBroadcastListeners() {

        generateUpdateEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && (
                        intent.getAction().equals(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION) ||
                intent.getAction().equals(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION))) {
                    String messageId = intent.getStringExtra(Conversation.ID);
                    if(conversationsViewModel != null) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Conversation conversation = conversationsViewModel
                                        .conversationDao.getMessage(messageId);
                                conversation.setRead(true);
                                try {
                                    if(E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                            E2EEHandler.deriveKeystoreAlias(
                                                    conversation.getAddress(), 0))) {
                                        informSecured(true);
                                    }
                                } catch (CertificateException | KeyStoreException | IOException |
                                         NoSuchAlgorithmException | NumberParseException e) {
                                    e.printStackTrace();
                                }
                                conversationsViewModel.update(conversation);
                            }
                        });
                    }
                    if(threadedConversationsViewModel != null) {
                        threadedConversationsViewModel.refresh(context);
                    }
                }  else {
                    String messageId = intent.getStringExtra(Conversation.ID);
                    if(conversationsViewModel != null && messageId != null) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Conversation conversation = conversationsViewModel
                                        .conversationDao.getMessage(messageId);
                                conversation.setRead(true);
                                try {
                                    if(E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                            E2EEHandler.deriveKeystoreAlias( conversation.getAddress(),
                                                    0))) {
                                        informSecured(true);
                                    }
                                } catch (CertificateException | KeyStoreException | IOException |
                                         NoSuchAlgorithmException | NumberParseException e) {
                                    e.printStackTrace();
                                }
                                conversationsViewModel.update(conversation);
                            }
                        });
                    }
                    if(threadedConversationsViewModel != null) {
                        threadedConversationsViewModel.refresh(context);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION);

        intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_UPDATED_BROADCAST_INTENT);
        intentFilter.addAction(DRAFT_PRESENT_BROADCAST);
        intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_UPDATED_BROADCAST_INTENT);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        else
            registerReceiver(generateUpdateEventsBroadcastReceiver, intentFilter);
    }

    protected void informSecured(boolean secured) { }

    private void cancelAllNotifications(int id) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(id);
    }

    protected void sendTextMessage(Conversation conversation,
                                   ThreadedConversations threadedConversations, String messageId) throws NumberParseException, InterruptedException {
        sendTextMessage(conversation.getText(),
                conversation.getSubscription_id(),
                threadedConversations, messageId);
    }

    protected void sendTextMessage(final String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId) throws NumberParseException, InterruptedException {
        if(text != null) {
            if(messageId == null)
                messageId = String.valueOf(System.currentTimeMillis());
            Conversation conversation = new Conversation();
            conversation.setMessage_id(messageId);
            conversation.setText(text);
            conversation.setThread_id(threadedConversations.getThread_id());
            conversation.setSubscription_id(subscriptionId);
            conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
            conversation.setDate(String.valueOf(System.currentTimeMillis()));
            conversation.setAddress(threadedConversations.getAddress());
            conversation.setStatus(Telephony.Sms.STATUS_PENDING);

            if(conversationsViewModel != null) {
                final String _messageId = messageId;
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            conversationsViewModel.insert(conversation);
                            SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, null);
//                            conversationsViewModel.updateThreadId(conversation.getThread_id(),
//                                    _messageId, id);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    protected void saveDraft(final String messageId, final String text, ThreadedConversations threadedConversations) throws InterruptedException {
        if(text != null) {
            if(conversationsViewModel != null) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        Conversation conversation = new Conversation();
                        conversation.setMessage_id(messageId);
                        conversation.setThread_id(threadedConversations.getThread_id());
                        conversation.setText(text);
                        conversation.setRead(true);
                        conversation.setType(Telephony.Sms.MESSAGE_TYPE_DRAFT);
                        conversation.setDate(String.valueOf(System.currentTimeMillis()));
                        conversation.setAddress(threadedConversations.getAddress());
                        conversation.setStatus(Telephony.Sms.STATUS_PENDING);
                        try {
                            conversationsViewModel.insert(conversation);

                            ThreadedConversations tc =
                                    ThreadedConversations.build(getApplicationContext(), conversation);
                            ThreadedConversationsDao threadedConversationsDao =
                                    tc.getDaoInstance(getApplicationContext());
                            threadedConversationsDao.insert(tc);
                            tc.close();

                            SMSDatabaseWrapper.saveDraft(getApplicationContext(), conversation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(DRAFT_PRESENT_BROADCAST);
                        sendBroadcast(intent);
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
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

    protected void cancelNotifications(String threadId) {
        if (!threadId.isEmpty()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                    getApplicationContext());
            notificationManager.cancel(Integer.parseInt(threadId));
        }
    }



}
