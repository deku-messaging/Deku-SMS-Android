package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryptionDao;
import com.afkanerd.deku.E2EE.E2EEHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {

    public ThreadedConversationsDao threadedConversationsDao;
    int pageSize = 20;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = 2 * pageSize;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

    MutableLiveData<Integer> inboxCount = new MutableLiveData<>();
    MutableLiveData<Integer> draftsCount = new MutableLiveData<>();

    ThreadsPagingSource threadsPagingSource;

    public LiveData<PagingData<ThreadedConversations>> getDrafts(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.threadedConversationsDao.getThreadedDrafts(
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> get(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.threadedConversationsDao.getAllWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getEncrypted(Context context) throws InterruptedException {
        List<String> address = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                        ConversationsThreadsEncryption.getDao(context);
                List<ConversationsThreadsEncryption> conversationsThreadsEncryptionList =
                        conversationsThreadsEncryptionDao.getAll();

                for(ConversationsThreadsEncryption conversationsThreadsEncryption :
                        conversationsThreadsEncryptionList) {
                    String derivedAddress =
                            E2EEHandler.getAddressFromKeystore(
                                    conversationsThreadsEncryption.getKeystoreAlias());
                    address.add(derivedAddress);
                }
            }
        });
        thread.start();
        thread.join();

        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getByAddress(address));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getNotEncrypted(Context context) throws InterruptedException {
        List<String> address = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                        ConversationsThreadsEncryption.getDao(context);
                List<ConversationsThreadsEncryption> conversationsThreadsEncryptionList =
                        conversationsThreadsEncryptionDao.getAll();

                for(ConversationsThreadsEncryption conversationsThreadsEncryption :
                        conversationsThreadsEncryptionList) {
                    String derivedAddress =
                            E2EEHandler.getAddressFromKeystore(
                                    conversationsThreadsEncryption.getKeystoreAlias());
                    address.add(derivedAddress);
                }
            }
        });
        thread.start();
        thread.join();
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getNotInAddress(address));
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

    public void filterInsert(Context context, List<ThreadedConversations> threadedConversations,
                                              List<ThreadedConversations> completeList) {
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
    }

    public void reset(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Conversation conversation = new Conversation();
                Cursor cursor = NativeSMSDB.fetchAll(context);

                List<Conversation> conversationList = new ArrayList<>();
                if(cursor != null && cursor.moveToFirst()) {
                    do {
                        conversationList.add(Conversation.build(cursor));
                    } while(cursor.moveToNext());
                    cursor.close();
                }

                ConversationDao conversationDao = conversation.getDaoInstance(context);
                conversationDao.insertAll(conversationList);

                threadedConversationsDao.deleteAll();
                refresh(context);

            }
        }).start();
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
                List<String> ids = new ArrayList<>();
                Conversation conversation = new Conversation();
                ConversationDao conversationDao = conversation.getDaoInstance(context);
                for(ThreadedConversations threadedConversation : threadedConversations)
                    ids.add(threadedConversation.getThread_id());

                conversationDao.deleteAll(ids);
                threadedConversationsDao.delete(threadedConversations);
                NativeSMSDB.deleteThreads(context, ids.toArray(new String[0]));
                conversation.close();
            }
        }).start();
    }

    public void refresh(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                Cursor cursor = context.getContentResolver().query(
                        Telephony.Threads.CONTENT_URI,
                        null,
                        null,
                        null,
                        "date DESC"
                );

                List<ThreadedConversations> threadedDraftsList =
                        threadedConversationsDao.getThreadedDraftsList(
                                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
                List<String> threadIds = new ArrayList<>();
                for(ThreadedConversations threadedConversations : threadedDraftsList)
                    threadIds.add(threadedConversations.getThread_id());

                if(cursor != null && cursor.moveToFirst()) {
                    do {
                        ThreadedConversations threadedConversations = new ThreadedConversations();
                        int recipientIdIndex = cursor.getColumnIndex("address");
                        int snippetIndex = cursor.getColumnIndex("body");
                        int dateIndex = cursor.getColumnIndex("date");
                        int threadIdIndex = cursor.getColumnIndex("thread_id");
                        int typeIndex = cursor.getColumnIndex("type");
                        int readIndex = cursor.getColumnIndex("read");

                        threadedConversations.setAddress(cursor.getString(recipientIdIndex));
                        if(threadedConversations.getAddress() == null || threadedConversations.getAddress().isEmpty())
                            continue;
                        threadedConversations.setThread_id(cursor.getString(threadIdIndex));
                        if(threadIds.contains(threadedConversations.getThread_id())) {
                            ThreadedConversations tc = threadedDraftsList.get(
                                    threadIds.indexOf(threadedConversations.getThread_id()));
                            threadedConversations.setSnippet(tc.getSnippet());
                            threadedConversations.setType(tc.getType());
                            threadedConversations.setDate(tc.getDate());
                        }
                        else {
                            threadedConversations.setSnippet(cursor.getString(snippetIndex));
                            threadedConversations.setType(cursor.getInt(typeIndex));
                            threadedConversations.setDate(cursor.getString(dateIndex));
                        }
                        String contactName = Contacts.retrieveContactName(context,
                                threadedConversations.getAddress());
                        threadedConversations.setContact_name(contactName);
                        threadedConversations.setIs_read(cursor.getInt(readIndex) == 1);

                        threadedConversationsList.add(threadedConversations);
                    } while(cursor.moveToNext());
                    cursor.close();
                }
                threadedConversationsDao.insertAll(threadedConversationsList);
                inboxCount.postValue(threadedConversationsList.size());
                draftsCount.postValue(threadIds.size());
            }
        }).start();
    }
}
