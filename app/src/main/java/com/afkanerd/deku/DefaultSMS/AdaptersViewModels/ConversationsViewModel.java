package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;


import android.content.Context;
import android.database.Cursor;
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

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConversationsViewModel extends ViewModel {
    public String threadId;
    public String address;
    ConversationDao conversationDao;
    public int pageSize = 10;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = pageSize * 2;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

    public Integer initialKey = null;

    ConversationPagingSource conversationPagingSource;

    LifecycleOwner lifecycleOwner;

    MutableLiveData<PagingData<Conversation>> mutableLiveData;
    public LiveData<PagingData<Conversation>> getSearch(ConversationDao conversationDao,
                                                        String threadId, Integer initialKey) {
        this.initialKey = initialKey;
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        conversationPagingSource = new ConversationPagingSource();
        Pager<Integer, Conversation> pager =
                conversationPagingSource.getRoomPaging(conversationDao, threadId, initialKey);
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }
    public void refresh(Integer initialKey) {
        this.initialKey = initialKey;
        conversationPagingSource.pagingSource.invalidate();
    }

    public LiveData<PagingData<Conversation>> get(ConversationDao conversationDao, String threadId)
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

    public long insert(Conversation conversation) throws InterruptedException {
        return conversationDao.insert(conversation);
    }

    public void update(Conversation conversation) {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                Conversation conversation1 = conversationDao.getMessage(conversation.getMessage_id());
//                conversation.setId(conversation1.getId());
                int numberUpdated = conversationDao.update(conversation);
                Log.d(getClass().getName(), "ROOM updated: " + numberUpdated);
            }
        }).start();
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
        Thread searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversation> list = conversationDao.getAll(threadId);

                for(int i=0;i<list.size();++i) {
                    if(list.get(i).getText().toLowerCase().contains(input.toLowerCase()))
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NativeSMSDB.Incoming.update_read(context, 1, threadId, null);
                    List<Conversation> conversations = conversationDao.getAll(threadId);
                    List<Conversation> updateList = new ArrayList<>();
                    for(Conversation conversation : conversations) {
                        if(!conversation.isRead()) {
                            conversation.setRead(true);
                            updateList.add(conversation);
                        }
                    }
                    conversationDao.update(updateList);
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

    public void updateThreadId(String threadId, String messageId, long id) {
        this.threadId = threadId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Conversation conversation = conversationDao.getMessage(messageId);
                conversation.setId(id);
                conversation.setThread_id(threadId);
                conversationDao.update(conversation);
            }
        }).start();
    }
}
