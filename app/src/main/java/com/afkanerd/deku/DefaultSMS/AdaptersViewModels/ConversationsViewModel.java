package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;


import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConversationsViewModel extends ViewModel {
    public String threadId;
    public String address;
    public ConversationDao conversationDao;
    public int pageSize = 10;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = pageSize * 2;

    public Integer initialKey = null;

    List<Integer> positions = new ArrayList<>();
    int pointer = 0;
    Pager<Integer, Conversation> pager;

    public LiveData<PagingData<Conversation>> getSearch(Context context, ConversationDao conversationDao,
                                                        String threadId, List<Integer> positions) {
        int pageSize = 5;
        int prefetchDistance = 3 * pageSize;
        boolean enablePlaceholder = false;
        int initialLoadSize = 10;
        this.conversationDao = conversationDao;
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

    PagingSource<Integer, Conversation> searchPagingSource;
    public PagingSource<Integer, Conversation> getNewConversationPagingSource(Context context) {
        searchPagingSource = new ConversationPagingSource(context, this.conversationDao, threadId,
                pointer >= this.positions.size()-1 ? null : this.positions.get(++pointer));
        return searchPagingSource;
    }

    public LiveData<PagingData<Conversation>> get(Context context, ConversationDao conversationDao,
                                                  String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

//        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
//                pageSize,
//                prefetchDistance,
//                enablePlaceholder,
//                initialLoadSize
//        ), ()-> this.conversationDao.get(threadId));
//        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);

        pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), null, ()->getNewConversationPagingSource(context));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public Conversation fetch(String messageId) throws InterruptedException {
        return conversationDao.getMessage(messageId);
    }

    public long insert(Conversation conversation) throws InterruptedException {
        long id = conversationDao.insert(conversation);
        searchPagingSource.invalidate();
        return id;
    }

    public void update(Conversation conversation) {
        conversationDao.update(conversation);
        searchPagingSource.invalidate();
    }

    public void insertFromNative(Context context, String messageId) throws InterruptedException {
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
            Conversation conversation1 = Conversation.build(cursor);
            cursor.close();
            update(conversation1);
        }
    }

    public List<Integer> search(String input) throws InterruptedException {
        List<Integer> positions = new ArrayList<>();
        List<Conversation> list = conversationDao.getAll(threadId);

        for(int i=0;i<list.size();++i) {
            if(list.get(i).getText() != null)
                if(list.get(i).getText().toLowerCase().contains(input.toLowerCase()))
                    positions.add(i);
        }

        return positions;
    }

    public void updateToRead(Context context) {
        if(threadId != null && !threadId.isEmpty()) {
            Conversation conversation1 = new Conversation();
            ConversationDao conversationDao = conversation1.getDaoInstance(context);

            List<Conversation> conversations = conversationDao.getAll(threadId);
            List<Conversation> updateList = new ArrayList<>();
            for(Conversation conversation : conversations) {
                if(!conversation.isRead()) {
                    conversation.setRead(true);
                    updateList.add(conversation);
                }
            }
            conversationDao.update(updateList);
            conversation1.close();
        }
    }

    public void deleteItems(Context context, List<Conversation> conversations) {
        Conversation conversation1 = new Conversation();
        ConversationDao conversationDao = conversation1.getDaoInstance(context);

        conversationDao.delete(conversations);
        String[] ids = new String[conversations.size()];
        for(int i=0;i<conversations.size(); ++i)
            ids[i] = conversations.get(i).getMessage_id();
        NativeSMSDB.deleteMultipleMessages(context, ids);

        conversation1.close();
    }

    public Conversation fetchDraft(Context context) throws InterruptedException {
        Conversation conversation1 = new Conversation();
        Conversation conversation = conversation1.getDaoInstance(context).fetchTypedConversation(
                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT, threadId);
        conversation1.close();
        return conversation;
    }

    public void clearDraft(Context context) {
        Conversation conversation1 = new Conversation();
        ConversationDao conversationDao = conversation1.getDaoInstance(context);
        conversationDao.deleteAllType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT,
                threadId);
        SMSDatabaseWrapper.deleteDraft(context, threadId);
        conversation1.close();
    }
}
