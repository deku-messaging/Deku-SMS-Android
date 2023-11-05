package com.afkanerd.deku.DefaultSMS;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationWorkManager;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.Router.Router.RouterWorkManager;

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

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPreferences.getBoolean(LOAD_NATIVES, true)) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.threading_conversations_natives_loaded), Toast.LENGTH_LONG).show();
            loadConversationsNativesBg();
        }
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

    public void configureBroadcastListeners(Object obj) {
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
                    else if(messageId != null && obj instanceof ConversationsViewModel) {
                        ConversationsViewModel viewModel = (ConversationsViewModel) obj;
                        Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
                        if(cursor.moveToFirst()) {
                            Conversation conversation = Conversation.build(cursor);
                            viewModel.insert(conversation);
                        }
                        cursor.close();
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
