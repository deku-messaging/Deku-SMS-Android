package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;

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
}
