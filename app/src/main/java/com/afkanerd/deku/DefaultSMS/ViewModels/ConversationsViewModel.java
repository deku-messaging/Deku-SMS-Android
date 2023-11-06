package com.afkanerd.deku.DefaultSMS.ViewModels;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
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

import java.util.ArrayList;
import java.util.List;

public class ConversationsViewModel extends ViewModel{
    public String threadId;
    ConversationDao conversationDao;
    public int pageSize = 10;
    int prefetchDistance = 30;
    boolean enablePlaceholder = false;
    public int initialLoadSize = 20;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;
    int jumpThreshold = 10;

    public LiveData<PagingData<Conversation>> get(ConversationDao conversationDao, String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;


        Pager<Integer, Conversation> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize,
                jumpThreshold
        ), ()-> this.conversationDao.get(threadId));
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
            update(conversation1);
        }
        cursor.close();
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
}
