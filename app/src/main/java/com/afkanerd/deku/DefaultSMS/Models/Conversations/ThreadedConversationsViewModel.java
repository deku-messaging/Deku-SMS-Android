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
        int initialLoadSize = 14;

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }


    public void insert(ThreadedConversations threadedConversations) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadedConversationsDao.insert(threadedConversations);
            }
        }).start();
    }

    public void loadNatives(Context context) {
        Thread loadNativeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = NativeSMSDB.fetchAll(context);
                threadedConversationsDao.insertAll(ThreadedConversations.buildRaw(cursor));
                cursor.close();
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

}
