package com.afkanerd.deku.E2EE;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Fragments.ModalSheetFragment;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    protected ThreadedConversations threadedConversations;
    View securePopUpRequest;
    boolean isEncrypted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void attachObservers() {
        try {
            final String keystoreAlias =
                    E2EEHandler.deriveKeystoreAlias(threadedConversations.getAddress(), 0);
            databaseConnector.conversationsThreadsEncryptionDao().fetchLiveData(keystoreAlias)
                    .observe(this, new Observer<ConversationsThreadsEncryption>() {
                        @Override
                        public void onChanged(ConversationsThreadsEncryption conversationsThreadsEncryption) {
                            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final boolean isSelf = E2EEHandler
                                                .isSelf(getApplicationContext(), keystoreAlias);

                                        threadedConversations.setSelf(isSelf);

                                        String _keystoreAlias = threadedConversations.isSelf() ?
                                                E2EEHandler.buildForSelf(keystoreAlias) :
                                                keystoreAlias;

                                        if(E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                                _keystoreAlias)) {
                                            informSecured(true);
                                        }
                                        else {
                                            showSecureRequestAgreementModal();
                                        }
                                    } catch (CertificateException | KeyStoreException |
                                             NoSuchAlgorithmException | IOException |
                                             UnrecoverableEntryException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
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
    public void sendTextMessage(final String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId,
                                final byte[] _mk) throws NumberParseException, InterruptedException {
        if(threadedConversations.is_secured && !isEncrypted) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String keystoreAlias =
                                E2EEHandler.deriveKeystoreAlias(
                                        threadedConversations.getAddress(), 0);
                        byte[][] cipherText = E2EEHandler.encrypt(getApplicationContext(),
                                keystoreAlias, text.getBytes(StandardCharsets.UTF_8),
                                threadedConversations.isSelf());
                        String encryptedText = E2EEHandler.buildTransmissionText(cipherText[0]);
                        isEncrypted = true;
                        sendTextMessage(encryptedText, subscriptionId, threadedConversations,
                                messageId, cipherText[1]);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            isEncrypted = false;
            super.sendTextMessage(text, subscriptionId, threadedConversations, messageId, _mk);
        }
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
                } else {
                    layout.setPlaceholderText(null);
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
                            E2EEHandler.buildForEncryptionRequest(getApplicationContext(),
                                    threadedConversations.getAddress(), null);

                    final String messageId = String.valueOf(System.currentTimeMillis());
                    Conversation conversation = new Conversation();
                    conversation.setThread_id(threadedConversations.getThread_id());
                    conversation.setAddress(threadedConversations.getAddress());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.conversation_secure_popup_request_menu_title));

        View conversationSecurePopView = View.inflate(getApplicationContext(),
                R.layout.conversation_secure_popup_menu, null);
        builder.setView(conversationSecurePopView);

        Button yesButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_send);
        Button cancelButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_cancel);
        TextView descriptionText = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_text_description);
        String descriptionTextRevised = descriptionText.getText()
                .toString()
                .replaceAll("\\[contact name]", threadedConversations.getContact_name() == null ?
                        threadedConversations.getAddress() : threadedConversations.getContact_name());
        descriptionText.setText(descriptionTextRevised);

        AlertDialog dialog = builder.create();

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataMessage(threadedConversations);
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

    public void setEncryptionThreadedConversations(ThreadedConversations threadedConversations) {
        this.threadedConversations = threadedConversations;
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
        ModalSheetFragment modalSheetFragment = new ModalSheetFragment(threadedConversations);
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(ModalSheetFragment.TAG);
        if(fragment == null || !fragment.isAdded())
            modalSheetFragment.show(getSupportFragmentManager(), ModalSheetFragment.TAG);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                if(!threadedConversations.isIs_secured()) {
                    try {
                        String keystoreAlias =
                                E2EEHandler.deriveKeystoreAlias(threadedConversations.getAddress(),
                                        0);
                        String _keystoreAlias = threadedConversations.isSelf() ?
                                E2EEHandler.buildForSelf(keystoreAlias) :
                                keystoreAlias;
                        if(!E2EEHandler.isAvailableInKeystore(_keystoreAlias) &&
                                E2EEHandler.fetchStoredPeerData(getApplicationContext(), keystoreAlias)
                                        != null) {
                            showSecureRequestAgreementModal();
                        }
                    } catch (NumberParseException | CertificateException | KeyStoreException |
                             IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
