package com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Fragments.ConversationsSecureRequestModalSheetFragment;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    View securePopUpRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void attachObservers() {
        try {
            final String keystoreAlias =
                    E2EEHandler.deriveKeystoreAlias(address, 0);
            databaseConnector.conversationsThreadsEncryptionDao().fetchLiveData(keystoreAlias)
                    .observe(this, new Observer<ConversationsThreadsEncryption>() {
                        @Override
                        public void onChanged(ConversationsThreadsEncryption conversationsThreadsEncryption) {
                            if(conversationsThreadsEncryption != null &&
                                    conversationsThreadsEncryption.getKeystoreAlias()
                                            .equals(keystoreAlias)) {
                                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final boolean isSelf = E2EEHandler
                                                    .isSelf(getApplicationContext(), keystoreAlias);

                                            String _keystoreAlias = isSelf ?
                                                    E2EEHandler.buildForSelf(keystoreAlias) :
                                                    keystoreAlias;

                                            ThreadedConversations threadedConversations =
                                                    databaseConnector.threadedConversationsDao()
                                                            .get(threadId);

                                            threadedConversations.setSelf(isSelf);
                                            if(E2EEHandler.canCommunicateSecurely(
                                                    getApplicationContext(), _keystoreAlias,
                                                    true)) {
                                                threadedConversations.setIs_secured(true);
                                                informSecured(true);
                                            }
                                            else {
                                                showSecureRequestAgreementModal();
                                            }
                                            databaseConnector.threadedConversationsDao()
                                                    .update(getApplicationContext(),
                                                            threadedConversations);
                                        } catch (CertificateException | KeyStoreException |
                                                 NoSuchAlgorithmException | IOException |
                                                 UnrecoverableEntryException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            else {
                                informSecured(false);
                            }
                        }
                    });
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param text
     * @param subscriptionId
     * @param threadedConversations
     * @param messageId
     * @param _mk
     * @throws NumberParseException
     * @throws InterruptedException
     */
    @Override
    public void sendTextMessage(String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId,
                                byte[] _mk) throws NumberParseException, InterruptedException {
        if(threadedConversations.is_secured) {
            try {
                String keystoreAlias =
                        E2EEHandler.deriveKeystoreAlias(
                                threadedConversations.getAddress(), 0);
                byte[][] cipherText = E2EEHandler.encrypt(getApplicationContext(),
                        keystoreAlias, text.getBytes(StandardCharsets.UTF_8),
                        threadedConversations.isSelf());
                text = E2EEHandler.buildTransmissionText(cipherText[0]);
                _mk = cipherText[1];
            } catch (Throwable e) {
                Log.e(E2EECompactActivity.class.getName(), "Exception", e);
            }
        }
        super.sendTextMessage(text, subscriptionId, threadedConversations, messageId, _mk);
    }

    @Override
    public void informSecured(boolean secured) {
        TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(secured && securePopUpRequest != null) {
                    securePopUpRequest.setVisibility(View.GONE);
                    layout.setPlaceholderText(getString(R.string.send_message_secured_text_box_hint));
                    getSupportActionBar().setSubtitle(R.string.messages_thread_encrypted_content_label);
                } else {
                    layout.setPlaceholderText(getString(R.string.send_message_text_box_hint));
                    getSupportActionBar().setSubtitle(null);
                }
            }
        });
    }

    protected void sendDataMessage(ThreadedConversations threadedConversations) {
        final int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String,  byte[]> transmissionRequestKeyPair =
                            E2EEHandler.buildForEncryptionRequest(getApplicationContext(), address,
                                    null);

                    final String messageId = String.valueOf(System.currentTimeMillis());
                    Conversation conversation = new Conversation();
                    conversation.setThread_id(threadId);
                    conversation.setAddress(address);
                    conversation.setIs_key(true);
                    conversation.setMessage_id(messageId);
                    conversation.setData(Base64.encodeToString(transmissionRequestKeyPair.second,
                            Base64.DEFAULT));
                    conversation.setSubscription_id(subscriptionId);
                    conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                    conversation.setDate(String.valueOf(System.currentTimeMillis()));
                    conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                    long id = conversationsViewModel.insert(getApplicationContext(), conversation);
                    SMSDatabaseWrapper.send_data(getApplicationContext(), conversation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void showSecureRequestPopUpMenu() {
        View conversationSecurePopView = getLayoutInflater()
                .inflate(R.layout.conversation_secure_popup_menu, null);

//        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_main);
        AlertDialog.Builder builder = new AlertDialog.Builder(conversationSecurePopView.getContext());
        builder.setTitle(getString(R.string.conversation_secure_popup_request_menu_title));
        builder.setView(conversationSecurePopView);

        Button yesButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_send);
        Button cancelButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_cancel);
        TextView descriptionText = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_text_description);
        String descriptionTextRevised = descriptionText.getText()
                .toString()
                .replaceAll("\\[contact name]", contactName == null ? address : contactName);
        descriptionText.setText(descriptionTextRevised);

        AlertDialog dialog = builder.create();

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendDataMessage(Datastore.getDatastore(getApplicationContext())
                                .threadedConversationsDao()
                                .get(threadId));
                    }
                });
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSecureRequestPopUpMenu();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
        setSecurePopUpRequest();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.conversation_main_menu_encrypt_lock == item.getItemId()) {
            if(securePopUpRequest != null) {
                securePopUpRequest.setVisibility(securePopUpRequest.getVisibility() == View.VISIBLE ?
                        View.GONE : View.VISIBLE);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSecureRequestAgreementModal() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(ConversationsSecureRequestModalSheetFragment.TAG);
        ThreadedConversations threadedConversations = databaseConnector.threadedConversationsDao()
                .get(threadId);
        if(threadedConversations != null && (fragment == null || !fragment.isAdded())) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ConversationsSecureRequestModalSheetFragment conversationsSecureRequestModalSheetFragment = new ConversationsSecureRequestModalSheetFragment(threadedConversations,
                    contactName);
            fragmentTransaction.add(conversationsSecureRequestModalSheetFragment,
                    ConversationsSecureRequestModalSheetFragment.TAG);
            fragmentTransaction.show(conversationsSecureRequestModalSheetFragment);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragmentTransaction.commitNow();
                    conversationsSecureRequestModalSheetFragment.getView().findViewById(R.id.conversation_secure_request_agree_btn)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    conversationsSecureRequestModalSheetFragment.dismiss();
                                    agreeToSecure();
                                }
                            });
                }
            });
        }
    }

    private void agreeToSecure() {
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadedConversations threadedConversations =
                            Datastore.getDatastore(getApplicationContext())
                            .threadedConversationsDao().get(threadId);
                    String keystoreAlias = E2EEHandler
                            .deriveKeystoreAlias(address, 0);
                    if (threadedConversations.isSelf()) {
                        keystoreAlias = E2EEHandler.buildForSelf(keystoreAlias);
                        Pair<String, byte[]> keystorePair = E2EEHandler
                                .buildForEncryptionRequest(getApplicationContext(),
                                        threadedConversations.getAddress(), keystoreAlias);

                        byte[] transmissionKey = E2EEHandler
                                .extractTransmissionKey(keystorePair.second);
                        if (threadedConversations.isSelf())
                            E2EEHandler.insertNewAgreementKeyDefault(getApplicationContext(),
                                    transmissionKey, keystoreAlias);
                        threadedConversations.setIs_secured(true);
                        threadedConversations.setSelf(true);
                        Datastore.getDatastore(getApplicationContext())
                                .threadedConversationsDao()
                                .update(getApplicationContext(), threadedConversations);
                    } else
                        sendDataMessage(threadedConversations);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
