package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {
    String messagesType;
    ThreadedConversationsDao threadedConversationsDao;

    public LiveData<PagingData<ThreadedConversations>> get(ThreadedConversationsDao threadedConversationsDao){
        this.threadedConversationsDao = threadedConversationsDao;

        int pageSize = 10;
        int prefetchDistance = 30;
        boolean enablePlaceholder = false;
        int initialLoadSize = 20;

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public void loadNative(Context context) throws InterruptedException {
        Thread loadNativeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = NativeSMSDB.fetchAndBuildConversation(context);
                List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                if(cursor.moveToNext()) {
                    do {
                        ThreadedConversations threadedConversations = ThreadedConversations.build(cursor);
                        threadedConversationsList.add(threadedConversations);
                    } while(cursor.moveToNext());
                }
                cursor.close();
                threadedConversationsDao.insertAll(threadedConversationsList);
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

    public void insert(ThreadedConversations threadedConversations) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadedConversationsDao.insert(threadedConversations);
            }
        }).start();
    }

}
