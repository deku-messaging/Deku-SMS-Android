package com.afkanerd.deku.E2EE;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.R;

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

    }

    @Override
    protected void onResume() {
        super.onResume();
        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
        securePopUpRequest.setVisibility(View.VISIBLE);
    }
}
