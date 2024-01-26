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
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity;
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

    public LiveData<PagingData<ThreadedConversations>> getArchived(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.threadedConversationsDao.getArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }
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

    public LiveData<PagingData<ThreadedConversations>> getBlocked(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.threadedConversationsDao.getBlocked());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getUnread(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> this.threadedConversationsDao.getAllUnreadWithoutArchived());
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
        ConversationsThreadsEncryption conversationsThreadsEncryption1 =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption1.getDaoInstance(context);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
        ConversationsThreadsEncryption conversationsThreadsEncryption1 =
                new ConversationsThreadsEncryption();
        ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                conversationsThreadsEncryption1.getDaoInstance(context);
        List<ConversationsThreadsEncryption> conversationsThreadsEncryptionList =
                conversationsThreadsEncryptionDao.getAll();

        for(ConversationsThreadsEncryption conversationsThreadsEncryption :
                conversationsThreadsEncryptionList) {
            String derivedAddress =
                    E2EEHandler.getAddressFromKeystore(
                            conversationsThreadsEncryption.getKeystoreAlias());
            address.add(derivedAddress);
        }
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize
        ), ()-> this.threadedConversationsDao.getNotInAddress(address));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }


    public void insert(ThreadedConversations threadedConversations) {
        threadedConversationsDao.insert(threadedConversations);
    }

    public void reset(Context context) {
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

        ThreadedConversations threadedConversations = new ThreadedConversations();
        ThreadedConversationsDao threadedConversationsDao1 =
                threadedConversations.getDaoInstance(context);
        threadedConversationsDao1.deleteAll();
        threadedConversations.close();
        refresh(context);
    }

    public void archive(List<Archive> archiveList) {
        threadedConversationsDao.archive(archiveList);
    }


    public void delete(Context context, List<String> ids) {
        Conversation conversation = new Conversation();
        ConversationDao conversationDao = conversation.getDaoInstance(context);
        conversationDao.deleteAll(ids);
        threadedConversationsDao.delete(ids);
        NativeSMSDB.deleteThreads(context, ids.toArray(new String[0]));
        conversation.close();
    }

    public void refresh(Context context) {
        List<ThreadedConversations> newThreadedConversationsList = new ArrayList<>();
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

        List<String> archivedThreads = threadedConversationsDao.getArchivedList();

        List<String> threadsIdsInDrafts = new ArrayList<>();
        for(ThreadedConversations threadedConversations : threadedDraftsList)
            threadsIdsInDrafts.add(threadedConversations.getThread_id());

                /*
                [date, rr, sub, subject, ct_t, read_status, reply_path_present, body, type, msg_box,
                thread_id, sub_cs, resp_st, retr_st, text_only, locked, exp, m_id, retr_txt_cs, st,
                date_sent, read, ct_cls, m_size, rpt_a, address, sub_id, pri, tr_id, resp_txt, ct_l,
                m_cls, d_rpt, v, person, service_center, error_code, _id, m_type, status]
                 */
        List<ThreadedConversations> threadedConversationsList =
                threadedConversationsDao.getAll();
        if(cursor != null && cursor.moveToFirst()) {
            do {
                ThreadedConversations threadedConversations = new ThreadedConversations();
                int recipientIdIndex = cursor.getColumnIndex("address");
                int snippetIndex = cursor.getColumnIndex("body");
                int dateIndex = cursor.getColumnIndex("date");
                int threadIdIndex = cursor.getColumnIndex("thread_id");
                int typeIndex = cursor.getColumnIndex("type");
                int readIndex = cursor.getColumnIndex("read");

                threadedConversations.setIs_read(cursor.getInt(readIndex) == 1);

                threadedConversations.setAddress(cursor.getString(recipientIdIndex));
                if(threadedConversations.getAddress() == null || threadedConversations.getAddress().isEmpty())
                    continue;
                threadedConversations.setThread_id(cursor.getString(threadIdIndex));
                if(threadsIdsInDrafts.contains(threadedConversations.getThread_id())) {
                    ThreadedConversations tc = threadedDraftsList.get(
                            threadsIdsInDrafts.indexOf(threadedConversations.getThread_id()));
                    threadedConversations.setSnippet(tc.getSnippet());
                    threadedConversations.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
                    threadedConversations.setDate(
                            Long.parseLong(tc.getDate()) >
                                    Long.parseLong(cursor.getString(dateIndex)) ?
                                    tc.getDate() : cursor.getString(dateIndex));
                }
                else {
                    threadedConversations.setSnippet(cursor.getString(snippetIndex));
                    threadedConversations.setType(cursor.getInt(typeIndex));
                    threadedConversations.setDate(cursor.getString(dateIndex));
                }

                threadedConversations.setIs_archived(
                        archivedThreads.contains(threadedConversations.getThread_id()));

                String contactName = Contacts.retrieveContactName(context,
                        threadedConversations.getAddress());
                threadedConversations.setContact_name(contactName);

                /**
                 * Check things that change first
                 * - Read status
                 * - Drafts
                 */
                if(!threadedConversationsList.contains(threadedConversations)) {
                    newThreadedConversationsList.add(threadedConversations);
                }
            } while(cursor.moveToNext());
            cursor.close();
        }
        threadedConversationsDao.insertAll(newThreadedConversationsList);
        getCount();
    }

    public void unarchive(List<Archive> archiveList) {
        threadedConversationsDao.unarchive(archiveList);
    }

    public void clearDrafts(Context context) {
        SMSDatabaseWrapper.deleteAllDraft(context);
        threadedConversationsDao.clearDrafts(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
        refresh(context);
    }

    public boolean hasUnread(List<String> ids) {
        return threadedConversationsDao.getAllUnreadWithoutArchivedCount(ids) > 0;
    }

    public void markUnRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 0, threadIds.toArray(new String[0]));
        threadedConversationsDao.updateRead(0, threadIds);
        refresh(context);
    }

    public void markRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 1, threadIds.toArray(new String[0]));
        threadedConversationsDao.updateRead(1, threadIds);
        refresh(context);
    }

    public void markAllUnRead(Context context) {
        NativeSMSDB.Incoming.update_all_read(context, 0);
        threadedConversationsDao.updateRead(0);
        refresh(context);
    }

    public void markAllRead(Context context) {
        NativeSMSDB.Incoming.update_all_read(context, 1);
        threadedConversationsDao.updateRead(1);
        refresh(context);
    }

    public MutableLiveData<List<Integer>> folderMetrics = new MutableLiveData<>();
    private void getCount() {
        int draftsListCount = threadedConversationsDao
                .getThreadedDraftsListCount( Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
        int encryptedCount = threadedConversationsDao.getAllEncryptedCount();
        int unreadCount = threadedConversationsDao.getAllUnreadWithoutArchivedCount();
        int blockedCount = threadedConversationsDao.getAllBlocked();
        List<Integer> list = new ArrayList<>();
        list.add(draftsListCount);
        list.add(encryptedCount);
        list.add(unreadCount);
        list.add(blockedCount);
        folderMetrics.postValue(list);
    }
}
