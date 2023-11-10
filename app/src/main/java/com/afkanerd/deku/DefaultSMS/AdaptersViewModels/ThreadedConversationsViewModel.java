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
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;

import java.util.ArrayList;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {
    ThreadedConversationsDao threadedConversationsDao;
    int pageSize = 10;
    int prefetchDistance = 30;
    boolean enablePlaceholder = false;
    int initialLoadSize = 14;

    public LiveData<PagingData<ThreadedConversations>> get(){

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getEncrypted(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getAllEncrypted());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getNotEncrypted(){
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
                List<ThreadedConversations> completeList = threadedConversationsDao.getAll();

                List<ThreadedConversations> insertList = new ArrayList<>();
                for(ThreadedConversations threadedConversation : threadedConversations) {
                    String contactName = Contacts.retrieveContactName(context,
                            threadedConversation.getAddress());
                    threadedConversation.setContact_name(contactName);
                    if(!completeList.contains(threadedConversation)) {
                        insertList.add(threadedConversation);
                    } else {
                        ThreadedConversations oldThread =
                                completeList.get(completeList.indexOf(threadedConversation));
                        if(oldThread.diffReplace(threadedConversation))
                            insertList.add(oldThread);
                    }
                }

                List<ThreadedConversations> deleteList = new ArrayList<>();
                if(threadedConversations.isEmpty()) {
                    deleteList = completeList;
                } else {
                    for (ThreadedConversations threadedConversation : completeList) {
                        if (!threadedConversations.contains(threadedConversation)) {
                            deleteList.add(threadedConversation);
                        }
                    }
                    threadedConversationsDao.insertAll(insertList);
                }
                threadedConversationsDao.delete(deleteList);
                cursor.close();
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

    public void setThreadedConversationsDao(ThreadedConversationsDao threadedConversationsDao) {
        this.threadedConversationsDao = threadedConversationsDao;
    }

    public void archive(List<Archive> archiveList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadedConversationsDao.archive(archiveList);
            }
        }).start();
    }


    public void delete(Context context, List<ThreadedConversations> threadedConversations) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] ids = new String[threadedConversations.size()];
                for(int i=0; i<threadedConversations.size(); ++i)
                    ids[i] = threadedConversations.get(i).getThread_id();
                NativeSMSDB.deleteThreads(context, ids);
                threadedConversationsDao.delete(threadedConversations);
            }
        }).start();
    }

}
