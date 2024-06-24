package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;
import android.provider.BlockedNumberContract;
import android.provider.Telephony;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {

    int pageSize = 20;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = 2 * pageSize;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

    private Datastore databaseConnector;

    public LiveData<PagingData<ThreadedConversations>> getArchived(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }
    public LiveData<PagingData<ThreadedConversations>> getDrafts(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getThreadedDrafts(
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getBlocked(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getBlocked());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getMuted(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getMuted());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getUnread(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getAllUnreadWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> get(Context context){
        this.databaseConnector = Datastore.getDatastore(context);
        try {
            Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                    pageSize,
                    prefetchDistance,
                    enablePlaceholder,
                    initialLoadSize,
                    maxSize
            ), ()-> databaseConnector.threadedConversationsDao().getAllWithoutArchived());
            return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAllExport() {
        List<Conversation> conversations = databaseConnector.conversationDao().getComplete();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting().serializeNulls();

        Gson gson = gsonBuilder.create();
        return gson.toJson(conversations);
    }

    public LiveData<PagingData<ThreadedConversations>> getEncrypted() throws InterruptedException {
        List<String> address = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<ConversationsThreadsEncryption> conversationsThreadsEncryptionList =
                        databaseConnector.conversationsThreadsEncryptionDao().getAll();
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
        ), ()-> databaseConnector.threadedConversationsDao().getByAddress(address));
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public void insert(ThreadedConversations threadedConversations) {
        databaseConnector.threadedConversationsDao()._insert(threadedConversations);
    }

    public void reset(Context context) {
        Cursor cursor = NativeSMSDB.fetchAll(context);

        List<Conversation> conversationList = new ArrayList<>();
        if(cursor != null && cursor.moveToFirst()) {
            do {
                conversationList.add(Conversation.Companion.build(cursor));
            } while(cursor.moveToNext());
            cursor.close();
        }

        databaseConnector.conversationDao().insertAll(conversationList);
        refresh(context);
    }

    public void archive(List<Archive> archiveList) {
        databaseConnector.threadedConversationsDao().archive(archiveList);
    }

    public void archive(String id) {
        Archive archive = new Archive();
        archive.thread_id = id;
        archive.is_archived = true;
        databaseConnector.threadedConversationsDao()
                .archive(new ArrayList<>(Collections.singletonList(archive)));
    }


    public void delete(Context context, List<String> ids) {
        databaseConnector.threadedConversationsDao().delete(context, ids);
        NativeSMSDB.deleteThreads(context, ids.toArray(new String[0]));
    }

    private void refresh(Context context) {
        try{
            Cursor cursor = context.getContentResolver().query(
                    Telephony.Threads.CONTENT_URI,
                    null,
                    null,
                    null,
                    "date DESC"
            );

                /*
                [date, rr, sub, subject, ct_t, read_status, reply_path_present, body, type, msg_box,
                thread_id, sub_cs, resp_st, retr_st, text_only, locked, exp, m_id, retr_txt_cs, st,
                date_sent, read, ct_cls, m_size, rpt_a, address, sub_id, pri, tr_id, resp_txt, ct_l,
                m_cls, d_rpt, v, person, service_center, error_code, _id, m_type, status]
                 */
            List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
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
                    threadedConversations.setSnippet(cursor.getString(snippetIndex));
                    threadedConversations.setType(cursor.getInt(typeIndex));
                    threadedConversations.setDate(cursor.getString(dateIndex));
                    if(BlockedNumberContract.isBlocked(context, threadedConversations.getAddress()))
                        threadedConversations.setIs_blocked(true);

                    String contactName = Contacts.retrieveContactName(context,
                            threadedConversations.getAddress());
                    threadedConversations.setContact_name(contactName);
                    threadedConversationsList.add(threadedConversations);
                } while(cursor.moveToNext());
                cursor.close();
            }
            databaseConnector.threadedConversationsDao().deleteAll();
            databaseConnector.threadedConversationsDao().insertAll(threadedConversationsList);
            getCount();
        } catch(Exception e) {
            Log.e(getClass().getName(), "Exception refreshing", e);
            loadNative(context);
        }

    }

    private void loadNative(Context context) {
        try(Cursor cursor = NativeSMSDB.fetchAll(context)) {
            List<ThreadedConversations> threadedConversations =
                    ThreadedConversations.buildRaw(cursor);
            databaseConnector.threadedConversationsDao().insertAll(threadedConversations);
        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception loading native", e);
        }
    }

    public void unarchive(List<Archive> archiveList) {
        databaseConnector.threadedConversationsDao().unarchive(archiveList);
    }

    public void unblock(Context context, List<String> threadIds) {
        List<ThreadedConversations> threadedConversationsList =
                databaseConnector.threadedConversationsDao().getList(threadIds);
        for(ThreadedConversations threadedConversations : threadedConversationsList) {
            BlockedNumberContract.unblock(context, threadedConversations.getAddress());
            threadedConversations.setIs_blocked(false);
            databaseConnector.threadedConversationsDao().update(context, threadedConversations);
        }
    }

    public void clearDrafts(Context context) {
        SMSDatabaseWrapper.deleteAllDraft(context);
        databaseConnector.threadedConversationsDao()
                .clearDrafts(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
    }

    public boolean hasUnread(List<String> ids) {
        return databaseConnector.threadedConversationsDao().getCountUnread(ids) > 0;
    }

    public void markUnRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 0, threadIds.toArray(new String[0]));
        databaseConnector.threadedConversationsDao().updateRead(0, threadIds);
    }

    public void markRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 1, threadIds.toArray(new String[0]));
        databaseConnector.threadedConversationsDao().updateRead(1, threadIds);
    }

    public void markAllRead(Context context) {
        NativeSMSDB.Incoming.update_all_read(context, 1);
        databaseConnector.threadedConversationsDao().updateRead(1);
    }

    public MutableLiveData<List<Integer>> folderMetrics = new MutableLiveData<>();
    public void getCount() {
        int draftsListCount = databaseConnector.threadedConversationsDao()
                .getThreadedDraftsListCount( Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
        int encryptedCount = databaseConnector.threadedConversationsDao().getCountEncrypted();
        int unreadCount = databaseConnector.threadedConversationsDao().getCountUnread();
        int blockedCount = databaseConnector.threadedConversationsDao().getCountBlocked();
        int mutedCount = databaseConnector.threadedConversationsDao().getCountMuted();
        List<Integer> list = new ArrayList<>();
        list.add(draftsListCount);
        list.add(encryptedCount);
        list.add(unreadCount);
        list.add(blockedCount);
        list.add(mutedCount);
        folderMetrics.postValue(list);
    }

    public void unMute(List<String> threadIds) {
        databaseConnector.threadedConversationsDao().updateMuted(0, threadIds);
    }

    public void mute(List<String> threadIds) {
        databaseConnector.threadedConversationsDao().updateMuted(1, threadIds);
    }

    public void unMuteAll() {
        databaseConnector.threadedConversationsDao().updateUnMuteAll();
    }
}
