package com.afkanerd.deku.DefaultSMS;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.i18n.phonenumbers.NumberParseException;

import java.nio.charset.StandardCharsets;

public class CustomAppCompactActivity extends DualSIMConversationActivity {

    protected String address;
    protected String contactName;
    protected String threadId;
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

        databaseConnector = Datastore.getDatastore(getApplicationContext());
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

            final Conversation conversation = new Conversation();
            if(_mk != null) {
                try {
                    String keystoreAlias = E2EEHandler.deriveKeystoreAlias(getApplicationContext(),
                            threadedConversations.getAddress(), 0);
                    if(threadedConversations.isSelf())
                        keystoreAlias = E2EEHandler.buildForSelf(keystoreAlias);
                    byte[] cipherText = E2EEHandler.extractTransmissionText(text);
                    ConversationsThreadsEncryption conversationsThreadsEncryption =
                            databaseConnector.conversationsThreadsEncryptionDao()
                                    .fetch(keystoreAlias);
                    byte[] AD = Base64.decode(conversationsThreadsEncryption.getPublicKey(), Base64.NO_WRAP);
                    String plainText = new String(E2EEHandler.decrypt(getApplicationContext(),
                            keystoreAlias, cipherText, _mk, AD, threadedConversations.isSelf()),
                            StandardCharsets.UTF_8);
                    conversation.setText(plainText);
                    conversation.setIs_encrypted(true);
                } catch(Throwable e ) {
                    e.printStackTrace();
                    conversation.setText(text);
                }
            } else {
                conversation.setText(text);
            }

            final String messageIdFinal = messageId;
            conversation.setMessage_id(messageId);
            conversation.setThread_id(threadId);
            conversation.setSubscription_id(subscriptionId);
            conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
            conversation.setDate(String.valueOf(System.currentTimeMillis()));
            conversation.setAddress(address);
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
                        } catch(Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        try {
                            if(_mk == null)
                                SMSDatabaseWrapper.send_text(getApplicationContext(),
                                        conversation, null);
                            else
                                SMSDatabaseWrapper.send_text(getApplicationContext(), conversation,
                                        text, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            NativeSMSDB.Outgoing.register_failed(getApplicationContext(),
                                    messageIdFinal, 1);
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

    protected void saveDraft(final String messageId, final String text) throws InterruptedException {
        if(text != null) {
            if(conversationsViewModel != null) {
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        Conversation conversation = new Conversation();
                        conversation.setMessage_id(messageId);
                        conversation.setThread_id(threadId);
                        conversation.setText(text);
                        conversation.setRead(true);
                        conversation.setType(Telephony.Sms.MESSAGE_TYPE_DRAFT);
                        conversation.setDate(String.valueOf(System.currentTimeMillis()));
                        conversation.setAddress(address);
                        conversation.setStatus(Telephony.Sms.STATUS_PENDING);
                        try {
                            conversationsViewModel.insert(getApplicationContext(), conversation);
                            SMSDatabaseWrapper.saveDraft(getApplicationContext(), conversation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
