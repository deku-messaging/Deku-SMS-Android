package com.afkanerd.deku.DefaultSMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationWorkManager;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;

import java.util.concurrent.TimeUnit;

public class CustomAppCompactActivity extends AppCompatActivity {
    BroadcastReceiver nativeStateChangedBroadcastReceiver;

    public static final String TAG_NAME = "NATIVE_CONVERSATION_TAG";
    public static final String UNIQUE_WORK_NAME = "NATIVE_CONVERSATION_TAG_UNIQUE_WORK_NAME";
    public static final String LOAD_NATIVES = "LOAD_NATIVES";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        loadConversationsNativesBg();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                SharedPreferences sharedPreferences =
//                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                if(sharedPreferences.getBoolean(LOAD_NATIVES, true)) {
//                    loadConversationsNativesBg();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(),
//                                    getString(R.string.threading_conversations_natives_loaded), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

    private void loadConversationsNativesBg() {
        Constraints constraints = new Constraints.Builder()
                .build();
        OneTimeWorkRequest routeMessageWorkRequest =
                new OneTimeWorkRequest.Builder(ConversationWorkManager.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                .addTag(TAG_NAME)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                routeMessageWorkRequest);
    }
    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    public void configureBroadcastListeners(Object obj, Object obj1) {
        nativeStateChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if(intent.getAction().equals(NativeSMSDB.BROADCAST_STATUS_CHANGED_ACTION)) {
                    String messageId = intent.getStringExtra(NativeSMSDB.BROADCAST_CONVERSATION_ID_INTENT);
                    if(messageId != null && obj instanceof ConversationsViewModel) {
                        Log.d(getLocalClassName(), "Message state changed: " + messageId);
                        ConversationsViewModel viewModel = (ConversationsViewModel) obj;
                        viewModel.updateFromNative(getApplicationContext(), messageId);
                    }
                }
                else if(intent.getAction().equals(NativeSMSDB.BROADCAST_NEW_MESSAGE_ACTION)) {
                    String threadId = intent.getStringExtra(NativeSMSDB.BROADCAST_THREAD_ID_INTENT);
                    String messageId = intent.getStringExtra(NativeSMSDB.BROADCAST_CONVERSATION_ID_INTENT);

                    if(obj instanceof ThreadedConversationsViewModel) {
                        ThreadedConversationsViewModel viewModel =
                                (ThreadedConversationsViewModel) obj;
                        viewModel.loadNatives(getApplicationContext());
                    }
                    else if(obj instanceof ConversationsViewModel) {
                        ConversationsViewModel viewModel = (ConversationsViewModel) obj;
                        if(viewModel.threadId.equals(threadId)) {
                            if (obj1 instanceof ConversationsRecyclerAdapter) {
                                ConversationsRecyclerAdapter conversationsRecyclerAdapter =
                                        (ConversationsRecyclerAdapter) obj1;
                                conversationsRecyclerAdapter.refresh();
                                cancelAllNotifications(Integer.parseInt(threadId));
                            }
                        }
                    }
                }
            }
        };


        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            registerReceiver(nativeStateChangedBroadcastReceiver,
                    new IntentFilter(NativeSMSDB.BROADCAST_STATUS_CHANGED_ACTION),
                    Context.RECEIVER_EXPORTED);

            registerReceiver(nativeStateChangedBroadcastReceiver,
                    new IntentFilter(NativeSMSDB.BROADCAST_NEW_MESSAGE_ACTION),
                    Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(nativeStateChangedBroadcastReceiver,
                    new IntentFilter(NativeSMSDB.BROADCAST_STATUS_CHANGED_ACTION));

            registerReceiver(nativeStateChangedBroadcastReceiver,
                    new IntentFilter(NativeSMSDB.BROADCAST_NEW_MESSAGE_ACTION));
        }
    }

    private void cancelAllNotifications(int id) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(id);
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
