package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.flow.FlowCollector;

public class ConversationsViewModel extends ViewModel{
    public String threadId;
    public String address;
    ConversationDao conversationDao;
    int pageSize = 20;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = 2 * pageSize;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

    public LiveData<PagingData<Conversation>> get(ConversationDao conversationDao, String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.conversationDao.get(threadId));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<Conversation>> getForSearch(ConversationDao conversationDao, String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.conversationDao.get(threadId));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<Conversation>> getByAddress(ConversationDao conversationDao, String address)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.address = address;

        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.conversationDao.getByAddress(address));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
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
                Conversation conversation1 = conversationDao.getMessage(conversation.getMessage_id());
                conversation.setId(conversation1.getId());
                int numberUpdated = conversationDao.update(conversation);
                Log.d(getClass().getName(), "ROOM updated: " + numberUpdated);
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
            Conversation conversation1 = Conversation.build(cursor);
            cursor.close();
            update(conversation1);
        }
    }

    public List<Integer> search(String input) throws InterruptedException {
        List<Integer> positions = new ArrayList<>();
        Thread searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversation> list = conversationDao.getAll(threadId);

                for(int i=0;i<list.size();++i) {
                    if(list.get(i).getBody().contains(input))
                        positions.add(i);
                }
            }
        });
        searchThread.start();
        searchThread.join();

        return positions;
    }

    public void updateToRead(Context context) {
        if(threadId != null && !threadId.isEmpty()) {
            NativeSMSDB.Incoming.update_read(context, 1, threadId);
            ThreadedConversationsDao threadedConversationsDao = ThreadedConversations.getDao(context);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ThreadedConversations threadedConversations = threadedConversationsDao.get(threadId);
                    if(threadedConversations != null) {
                        threadedConversations.setIs_read(true);
                        int num_updated = threadedConversationsDao.update(threadedConversations);
                        Log.d(getClass().getName(), "Number updated: " + num_updated);
                    }
                }
            }).start();
        }
    }

    public void deleteItems(Context context, List<Conversation> conversations) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                conversationDao.delete(conversations);
                String[] ids = new String[conversations.size()];
                for(int i=0;i<conversations.size(); ++i)
                    ids[i] = conversations.get(i).getMessage_id();
                Log.d(getClass().getName(), "Pre-delete: " + Arrays.toString(ids));
                int deletedCount = NativeSMSDB.deleteMultipleMessages(context, ids);
                Log.d(getClass().getName(), "Deleted: " + deletedCount + ":" + Arrays.toString(ids) + ":" + conversations.size());
            }
        }).start();
    }
}
