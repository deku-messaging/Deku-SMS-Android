package com.afkanerd.deku.E2EE;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.Models.Transmissions;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    String address;
    View securePopUpRequest;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            address = savedInstanceState.getString(Conversation.ADDRESS);
        }
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int subscriptionId = 0;
                    String transmissionRequest =
                            E2EEHandler.buildForEncryptionRequest(getApplicationContext(), address);
                    SMSDatabaseWrapper.send_data(
                            getApplicationContext(),
                            String.valueOf(System.currentTimeMillis()),
                            address,
                            transmissionRequest.getBytes(StandardCharsets.UTF_8),
                            subscriptionId,
                            null);
                } catch (NumberParseException | GeneralSecurityException | IOException |
                         InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
        securePopUpRequest.setVisibility(View.VISIBLE);
        setSecurePopUpRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
