package com.afkanerd.deku.DefaultSMS;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_NAME;

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
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsWorker;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;
import com.afkanerd.deku.Router.Router.RouterWorkManager;

import java.util.concurrent.TimeUnit;

public class CustomAppCompactActivity extends AppCompatActivity {
    BroadcastReceiver nativeStateChangedBroadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }
        loadNatives();
    }


    private void loadNatives() {
        Constraints constraints = new Constraints.Builder().build();
        OneTimeWorkRequest loadNativeOneTimeWorkRequest = new OneTimeWorkRequest.Builder(ConversationsWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                .addTag(ConversationsWorker.TAG_NAME)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueueUniqueWork(
                ConversationsWorker.UNIQUE_WORK_ADDRESS,
                ExistingWorkPolicy.KEEP,
                loadNativeOneTimeWorkRequest);
    }

    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    public void configureBroadcastListeners(Object obj) {
        nativeStateChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if(intent.getAction().equals(NativeSMSDB.BROADCAST_STATUS_CHANGED_ACTION)) {
                    String threadId = intent.getStringExtra(NativeSMSDB.BROADCAST_THREAD_ID_INTENT);
                    String messageId = intent.getStringExtra(NativeSMSDB.BROADCAST_CONVERSATION_ID_INTENT);
                    if(threadId != null && obj instanceof ConversationsViewModel) {
                        ConversationsViewModel viewModel = (ConversationsViewModel) obj;
                        viewModel.updateFromNative(getApplicationContext(), messageId);
                    }
                }
                else if(intent.getAction().equals(NativeSMSDB.BROADCAST_NEW_MESSAGE_ACTION)) {
                    String threadId = intent.getStringExtra(NativeSMSDB.BROADCAST_THREAD_ID_INTENT);
                    String messageId = intent.getStringExtra(NativeSMSDB.BROADCAST_CONVERSATION_ID_INTENT);
                    if(threadId != null && obj instanceof ThreadedConversationsViewModel) {
                        ThreadedConversationsViewModel viewModel =
                                (ThreadedConversationsViewModel) obj;
                        Cursor cursor = NativeSMSDB.fetchByThreadId(context, threadId);
                        if(cursor.moveToFirst()) {
                            ThreadedConversations threadedConversations = ThreadedConversations.build(cursor);
                            viewModel.insert(threadedConversations);
                        }
                        cursor.close();
                    }

                }
            }
        };

        registerReceiver(nativeStateChangedBroadcastReceiver,
                new IntentFilter(NativeSMSDB.BROADCAST_STATUS_CHANGED_ACTION),
                Context.RECEIVER_EXPORTED);

        registerReceiver(nativeStateChangedBroadcastReceiver,
                new IntentFilter(NativeSMSDB.BROADCAST_NEW_MESSAGE_ACTION),
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
