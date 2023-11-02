package com.afkanerd.deku.DefaultSMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;

public class CustomAppCompactActivity extends AppCompatActivity {
    BroadcastReceiver nativeStateChangedBroadcastReceiver;

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

    ThreadedConversationsViewModel threadedConversationsViewModel;
    ConversationsViewModel conversationsViewModel;

    public void configureBroadcastListeners(Object obj) {
        nativeStateChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                String threadId = intent.getStringExtra(Conversation.BROADCAST_THREAD_ID_INTENT);
                if(threadId != null && obj instanceof ThreadedConversationsViewModel) {
                    ThreadedConversationsViewModel viewModel =
                            (ThreadedConversationsViewModel) obj;
                    Cursor cursor = SMSHandler.fetchByThreadId(context, threadId);
                    if(cursor.moveToFirst()) {
                        ThreadedConversations threadedConversations = ThreadedConversations.build(cursor);
                        viewModel.insert(threadedConversations);
                    }
                    cursor.close();
                }
                else if(threadId != null && obj instanceof ConversationsViewModel) {
                    ConversationsViewModel viewModel = (ConversationsViewModel) obj;
                    Cursor cursor = SMSHandler.fetchByThreadId(context, threadId);
                    if(cursor.moveToFirst()) {
                        Conversation conversation = Conversation.build(cursor);
                        viewModel.insert(conversation);
                    }
                    cursor.close();
                }
            }
        };

        registerReceiver(nativeStateChangedBroadcastReceiver,
                new IntentFilter(SMSHandler.NATIVE_STATE_CHANGED_BROADCAST_INTENT),
                Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nativeStateChangedBroadcastReceiver != null)
            unregisterReceiver(nativeStateChangedBroadcastReceiver);
    }

    public void cancelNotifications(String threadId) {
        if (!threadId.isEmpty()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                    getApplicationContext());
            notificationManager.cancel(Integer.parseInt(threadId));
        }
    }

}
