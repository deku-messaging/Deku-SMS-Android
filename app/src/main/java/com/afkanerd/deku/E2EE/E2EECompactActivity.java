package com.afkanerd.deku.E2EE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.preference.PreferenceManager;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.Models.SettingsHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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

    public static String INFORMED_SECURED = "INFORMED_SECURED";

    @Override
    public void sendTextMessage(final String text, int subscriptionId, ThreadedConversations threadedConversations) throws Exception {
        String keystoreAlias = E2EEHandler.getKeyStoreAlias(threadedConversations.getAddress(), 0);
        final String[] transmissionText = {text};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(E2EEHandler.canCommunicateSecurely(getApplicationContext(), keystoreAlias)) {
                        byte[] cipherText = E2EEHandler.encryptText(getApplicationContext(),
                                keystoreAlias, text);
                        transmissionText[0] = E2EEHandler.buildTransmissionText(cipherText);
                    }
                } catch (IOException | GeneralSecurityException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.join();

        super.sendTextMessage(transmissionText[0], subscriptionId, threadedConversations);
    }

    @Override
    public void informSecured(boolean secured) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(secured && securePopUpRequest != null) {
                    securePopUpRequest.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.conversation_inform_user_now_secured_toast),
                            Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataMessage(threadedConversations);
            }
        });
    }

    public void setEncryptionThreadedConversations(ThreadedConversations threadedConversations) {
        this.threadedConversations = threadedConversations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(threadedConversations != null &&
                            !E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                    E2EEHandler.getKeyStoreAlias(threadedConversations.getAddress(), 0))) {
                        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(SettingsHandler.alertNotEncryptedCommunicationDisabled(getApplicationContext())) {
                                    securePopUpRequest.setVisibility(View.GONE);
                                } else {
                                    securePopUpRequest.setVisibility(View.VISIBLE);
                                    setSecurePopUpRequest();
                                }
                            }
                        });
                    }
                } catch (CertificateException | NumberParseException | NoSuchAlgorithmException |
                         IOException | KeyStoreException e) {
                    e.printStackTrace();
                }

            }
        }).start();
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

    @Override
    protected void onResume() {
        super.onResume();
    }
}
