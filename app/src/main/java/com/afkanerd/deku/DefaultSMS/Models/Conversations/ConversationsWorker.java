package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class ConversationsWorker extends Worker {

    public final static String TAG_NAME = "load_native_work_manager";
    public final static String UNIQUE_WORK_ADDRESS = "load_native_unique_work_address";
    public ConversationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        loadThreadsFromNative();
        loadConversationsFromNative();
        return Result.success();
    }

    public void loadConversationsFromNative() {
        Cursor cursor = NativeSMSDB.fetchAll(getApplicationContext());
        List<Conversation> conversationList = new ArrayList<>();
        if(cursor.moveToNext()) {
            do {
                conversationList.add(Conversation.build(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        ConversationDao conversationDao = Conversation.getDao(getApplicationContext());
        conversationDao.insertAll(conversationList);
    }

    private void loadThreadsFromNative() {
        Cursor cursor = SMSHandler.fetchThreads(getApplicationContext());
        List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
        if(cursor.moveToNext()) {
            do {
                threadedConversationsList.add(ThreadedConversations.build(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        ThreadedConversationsDao threadedConversationsDao =
                ThreadedConversations.getDao(getApplicationContext());
        threadedConversationsDao.insert(threadedConversationsList);
    }
}
