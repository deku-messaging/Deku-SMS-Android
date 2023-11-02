package com.afkanerd.deku.DefaultSMS.Models.Conversations;


import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class ConversationsViewModel extends ViewModel{
    public String threadId;

    LiveData<PagingData<Conversation>> liveData;

    ConversationDao conversationDao;

    PagingLiveData pagingLiveData;

    public LiveData<PagingData<Conversation>> get(ConversationDao conversationDao, String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        int pageSize = 20;
        int prefetchDistance = 1;
        boolean enablePlaceholder = false;
        int initialLoadSize = 20;

//        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
//                pageSize,
//                prefetchDistance,
//                enablePlaceholder,
//                initialLoadSize
//        ), ()-> new ConversationPaging(this.conversationDao));

        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.conversationDao.get(threadId));
        liveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);

//        loadRoom();
        return this.liveData;
    }

    public void loadNative(Context context) {
        Thread loadNativeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = NativeSMSDB.fetchByThreadId(context, threadId);
                List<Conversation> conversationList = new ArrayList<>();
                if(cursor.moveToNext()) {
                    do {
                        conversationList.add(Conversation.build(cursor));
                    } while(cursor.moveToNext());
                }
                cursor.close();
                conversationDao.insertAll(conversationList);
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

    public void insert(Conversation conversation) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                conversationDao.insert(conversation);
            }
        }).start();
    }

    public void update(Conversation conversation) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                conversationDao.update(conversation);
            }
        }).start();
    }

    public void insertFromNative(Context context, String messageId) {
        Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
        if(cursor.moveToFirst()) {
            Conversation conversation = Conversation.build(cursor);
            insert(conversation);
        }
        cursor.close();
    }

    public void updateFromNative(Context context, String messageId ) {
        Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
        if(cursor.moveToFirst()) {
            Conversation conversation = Conversation.build(cursor);
            update(conversation);
        }
        cursor.close();
    }
}
