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
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomAppCompactActivity extends DualSIMConversationActivity {
    protected final static String DRAFT_PRESENT_BROADCAST = "DRAFT_PRESENT_BROADCAST";

    protected ConversationsViewModel conversationsViewModel;

    protected ThreadedConversationsViewModel threadedConversationsViewModel;


    public Datastore databaseConnector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        if(Datastore.datastore == null || !Datastore.datastore.isOpen()) {
            Log.d(getClass().getName(), "Yes I am closed");
            Datastore.datastore = Room.databaseBuilder(getApplicationContext(), Datastore.class,
                            Datastore.databaseName)
                    .enableMultiInstanceInvalidation()
                    .build();
        }
        databaseConnector = Datastore.datastore;
    }

    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
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
                threadedConversations, messageId, null);
    }

    protected void sendTextMessage(String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId,
                                   byte[] _mk) throws NumberParseException, InterruptedException {
        if(text != null) {
            if(messageId == null)
                messageId = String.valueOf(System.currentTimeMillis());

            Conversation conversation = new Conversation();
            if(_mk != null) {
                try {
                    String keystoreAlias = E2EEHandler.deriveKeystoreAlias(
                            threadedConversations.getAddress(), 0);
                    byte[] cipherText = E2EEHandler.extractTransmissionText(text);
                    String plainText = new String(E2EEHandler.decrypt(getApplicationContext(),
                            keystoreAlias, cipherText, _mk, null, false),
                            StandardCharsets.UTF_8);
                    conversation.setText(plainText);
                } catch(Throwable e ) {
                    e.printStackTrace();
                }
            } else {
                conversation.setText(text);
            }

            final String messageIdFinal = messageId;
            conversation.setMessage_id(messageId);
            conversation.setThread_id(threadedConversations.getThread_id());
            conversation.setSubscription_id(subscriptionId);
            conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
            conversation.setDate(String.valueOf(System.currentTimeMillis()));
            conversation.setAddress(threadedConversations.getAddress());
            conversation.setStatus(Telephony.Sms.STATUS_PENDING);
            // TODO: should encrypt this before storing
//            if(_mk != null)
//                conversation.set_mk(Base64.encodeToString(_mk, Base64.NO_WRAP));

            if(conversationsViewModel != null) {
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            conversationsViewModel.insert(getApplicationContext(), conversation);
                            if(_mk == null)
                                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation, null);
                            else
                                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation,
                                        text, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            NativeSMSDB.Outgoing.register_failed(getApplicationContext(), messageIdFinal, 1);
                            conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                            conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                            conversation.setError_code(1);
                            conversationsViewModel.update(conversation);
                        }
                    }
                });
            }
        }
    }

    protected void saveDraft(final String messageId, final String text, ThreadedConversations threadedConversations) throws InterruptedException {
        if(text != null) {
            if(conversationsViewModel != null) {
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                            conversationsViewModel.insert(getApplicationContext(), conversation);

                            ThreadedConversations tc =
                                    ThreadedConversations.build(getApplicationContext(), conversation);
                            databaseConnector.threadedConversationsDao().insert(tc);

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

    protected void cancelNotifications(String threadId) {
        if (!threadId.isEmpty()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                    getApplicationContext());
            notificationManager.cancel(Integer.parseInt(threadId));
        }
    }



}
