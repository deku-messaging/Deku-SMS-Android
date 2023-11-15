package com.afkanerd.deku.E2EE;

import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    ThreadedConversations threadedConversations;
    View securePopUpRequest;

    ConversationsViewModel conversationsViewModel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setViewModel(ConversationsViewModel viewModel) {
        super.setViewModel(viewModel);
        this.conversationsViewModel = viewModel;
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int subscriptionId = 0;

                    byte[] transmissionRequest =
                            E2EEHandler.buildForEncryptionRequest(getApplicationContext(),
                                    threadedConversations.getAddress());
                    final String messageId = String.valueOf(System.currentTimeMillis());
                    Conversation conversation = new Conversation();
                    conversation.setIs_key(true);
                    conversation.setMessage_id(messageId);
                    conversation.setData(Base64.encodeToString(transmissionRequest, Base64.DEFAULT));
                    conversation.setSubscription_id(subscriptionId);
                    conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                    conversation.setDate(String.valueOf(System.currentTimeMillis()));
                    conversation.setAddress(threadedConversations.getAddress());
                    conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                    long id = conversationsViewModel.insert(conversation);
                    SMSDatabaseWrapper.send_data(getApplicationContext(), conversation);
                    conversation.setId(id);
                    conversationsViewModel.update(conversation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setEncryptionThreadedConversations(ThreadedConversations threadedConversations) {
        this.threadedConversations = threadedConversations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(this.threadedConversations != null) {
            securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
            securePopUpRequest.setVisibility(View.VISIBLE);
            setSecurePopUpRequest();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
