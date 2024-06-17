package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;


import android.content.Context;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;

import java.util.ArrayList;
import java.util.List;

public class ConversationsViewModel extends ViewModel {
    public Datastore datastore;
    public String threadId;
    public String address;
    public int pageSize = 10;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = true;
    int initialLoadSize = pageSize * 2;

    public Integer initialKey = null;

    List<Integer> positions = new ArrayList<>();
    int pointer = 0;
    Pager<Integer, Conversation> pager;

    public LiveData<PagingData<Conversation>> getSearch(Context context, String threadId,
                                                        List<Integer> positions) {
        int pageSize = 5;
        int prefetchDistance = 3 * pageSize;
        boolean enablePlaceholder = false;
        int initialLoadSize = 10;
        this.threadId = threadId;
        this.positions = positions;

        pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), initialKey, ()-> getNewConversationPagingSource(context));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    PagingSource<Integer, Conversation> customPagingSource;
    public PagingSource<Integer, Conversation> getNewConversationPagingSource(Context context) {
        customPagingSource = new ConversationPagingSource(context, datastore.conversationDao(),
                threadId,
                pointer >= this.positions.size()-1 ? null : this.positions.get(++pointer));
        return customPagingSource;
    }

    public LiveData<PagingData<Conversation>> get(String threadId)
            throws InterruptedException {
        this.threadId = threadId;

        pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), null, ()->datastore.conversationDao().get(threadId));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public Conversation fetch(String messageId) throws InterruptedException {
        return datastore.conversationDao().getMessage(messageId);
    }

    public long insert(Context context, Conversation conversation) throws InterruptedException {
        datastore.threadedConversationsDao().insertThreadAndConversation(context, conversation);
        if(customPagingSource != null)
            customPagingSource.invalidate();
        return 0;
    }

    public void update(Conversation conversation) {
        datastore.conversationDao()._update(conversation);
        customPagingSource.invalidate();
    }

    public List<Integer> search(String input) throws InterruptedException {
        List<Integer> positions = new ArrayList<>();
        List<Conversation> list = datastore.conversationDao().getAll(threadId);

        for(int i=0;i<list.size();++i) {
            if(list.get(i).getText() != null)
                if(list.get(i).getText().toLowerCase().contains(input.toLowerCase()))
                    positions.add(i);
        }

        return positions;
    }

    public void updateInformation(Context context, String contactName) {
        datastore.conversationDao().updateRead(true, threadId);
        ThreadedConversations threadedConversations =
                datastore.threadedConversationsDao().get(threadId);
        if(threadedConversations != null) {
            threadedConversations.setContact_name(contactName);
            datastore.threadedConversationsDao().update(context, threadedConversations);
        }
    }

    public void deleteItems(Context context, List<Conversation> conversations) {
        datastore.conversationDao().delete(conversations);
        String[] ids = new String[conversations.size()];
        for(int i=0;i<conversations.size(); ++i)
            ids[i] = conversations.get(i).getMessage_id();
        NativeSMSDB.deleteMultipleMessages(context, ids);
    }

    public Conversation fetchDraft() throws InterruptedException {
        return datastore.conversationDao().fetchTypedConversation(
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT, threadId);
    }

    public void clearDraft(Context context) {
        datastore.conversationDao()
                .deleteAllType(context, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT, threadId);
        SMSDatabaseWrapper.deleteDraft(context, threadId);
    }

    public void unMute() {
        datastore.threadedConversationsDao().updateMuted(0, threadId);
    }

    public void mute() {
        datastore.threadedConversationsDao().updateMuted(1, threadId);
    }

}
