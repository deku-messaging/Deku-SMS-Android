package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;

import java.util.ArrayList;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {
    ThreadedConversationsDao threadedConversationsDao;

    public LiveData<PagingData<ThreadedConversations>> get(){
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

    public LiveData<PagingData<ThreadedConversations>> getEncrypted(){
        int pageSize = 10;
        int prefetchDistance = 30;
        boolean enablePlaceholder = false;
        int initialLoadSize = 14;

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllEncrypted());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getNotEncrypted(){
        int pageSize = 10;
        int prefetchDistance = 30;
        boolean enablePlaceholder = false;
        int initialLoadSize = 14;

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllNotEncrypted());
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
                List<ThreadedConversations> threadedConversations =
                        ThreadedConversations.buildRaw(cursor);
                for(ThreadedConversations threadedConversation : threadedConversations) {
                    String contactName = Contacts.retrieveContactName(context,
                            threadedConversation.getAddress());

                    if(contactName != null) {
                        threadedConversation.setContact_name(contactName);
                        threadedConversation.setAvatar_initials(contactName.substring(0, 1));
                        threadedConversation.setAvatar_color(Helpers.generateColor(contactName));
                    }
                    else threadedConversation.setAvatar_color(
                            Helpers.generateColor(threadedConversation.getAddress()));
                }
                if(!threadedConversations.isEmpty()) {
                    threadedConversationsDao.insertAll(threadedConversations);
                    List<ThreadedConversations> completeList = threadedConversationsDao.getAll();
                    List<ThreadedConversations> deleteList = new ArrayList<>();
                    for (ThreadedConversations threadedConversation : completeList) {
                        if (!threadedConversations.contains(threadedConversation)) {
                            deleteList.add(threadedConversation);
                        }
                    }
                    threadedConversationsDao.delete(deleteList);
                }
                cursor.close();
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

    public void setThreadedConversationsDao(ThreadedConversationsDao threadedConversationsDao) {
        this.threadedConversationsDao = threadedConversationsDao;
    }
}
