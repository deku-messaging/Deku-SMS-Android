package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;

import java.util.List;

public class ArchivedViewModel extends ViewModel {

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
        ), ()-> this.threadedConversationsDao.getArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public void unarchive(List<Archive> archiveList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadedConversationsDao.unarchive(archiveList);
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
