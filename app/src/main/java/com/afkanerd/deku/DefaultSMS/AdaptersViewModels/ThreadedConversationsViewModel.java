package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.BlockedNumberContract;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class ThreadedConversationsViewModel extends ViewModel {

    int pageSize = 20;
    int prefetchDistance = 3 * pageSize;
    boolean enablePlaceholder = false;
    int initialLoadSize = 2 * pageSize;
    int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

    public Datastore databaseConnector;

    public LiveData<PagingData<ThreadedConversations>> getArchived(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }
    public LiveData<PagingData<ThreadedConversations>> getDrafts(){
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

    public LiveData<PagingData<ThreadedConversations>> getBlocked(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getBlocked());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> getMuted(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getMuted());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    PagingSource<Integer, ThreadedConversations> mutedPagingSource;
    public LiveData<PagingData<ThreadedConversations>> getUnread(){
        Pager<Integer, ThreadedConversations> pager = new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), ()-> databaseConnector.threadedConversationsDao().getAllUnreadWithoutArchived());
        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), this);
    }

    public LiveData<PagingData<ThreadedConversations>> get(){
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
                        Datastore.datastore.conversationsThreadsEncryptionDao().getAll();
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
        databaseConnector.threadedConversationsDao().insert(threadedConversations);
    }

    public void reset(Context context) {
        Cursor cursor = NativeSMSDB.fetchAll(context);

        List<Conversation> conversationList = new ArrayList<>();
        if(cursor != null && cursor.moveToFirst()) {
            do {
                conversationList.add(Conversation.build(cursor));
            } while(cursor.moveToNext());
            cursor.close();
        }

        databaseConnector.conversationDao().insertAll(conversationList);
        databaseConnector.threadedConversationsDao().deleteAll();
        refresh(context);
    }

    public void archive(List<Archive> archiveList) {
        databaseConnector.threadedConversationsDao().archive(archiveList);
    }


    public void delete(Context context, List<String> ids) {
        databaseConnector.conversationDao().deleteAll(ids);
        databaseConnector.threadedConversationsDao().delete(ids);
        NativeSMSDB.deleteThreads(context, ids.toArray(new String[0]));
    }

    public static Cursor getNewestMessages(Context context, List<String> threadIds) {
        ContentResolver contentResolver = context.getContentResolver();

        // Build the selection string using IN operator
        StringBuilder selectionBuilder = new StringBuilder(Telephony.Sms.THREAD_ID + " IN (");
        for (int i = 0; i < threadIds.size(); i++) {
            selectionBuilder.append("?");
            if (i < threadIds.size() - 1) {
                selectionBuilder.append(",");
            }
        }
        selectionBuilder.append(")");

        // Prepare selection arguments
        String[] selectionArgs = threadIds.toArray(new String[0]);

        // Sort by date in descending order to get the newest message first
        String sortOrder = Telephony.Sms.DATE + " DESC";

        // Query the content provider
        return contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null, // projection: retrieve all columns
                selectionBuilder.toString(),
                selectionArgs,
                sortOrder
        );
    }

    private void refresh(Context context) {
//        [snippet, thread_id, msg_count]
        List<String> threadIds = new ArrayList<>();
        try(Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.Conversations.CONTENT_URI,
                null,
                null,
                null,
                "date DESC"
        )) {
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        int snippetIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);
                        int threadIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID);

                        String snippet = cursor.getString(snippetIndex);
                        String threadId = cursor.getString(threadIndex);
                        threadIds.add(threadId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while(cursor.moveToNext());

//                [_id, thread_id, address, person, date, date_sent, protocol, read, status, type,
//                reply_path_present, subject, body, service_center, locked, sub_id, error_code, creator, seen]
                Cursor conversationsCursor = getNewestMessages(context, threadIds);
                if(conversationsCursor != null && conversationsCursor.moveToFirst()) {
                    List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                    do {
                        try {
                            int dateIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.DATE);
                            int addressIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                            int typeIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.TYPE);
                            int readIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.READ);
                            int threadIdIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.THREAD_ID);
                            int bodyIndex = conversationsCursor
                                    .getColumnIndexOrThrow(Telephony.Sms.BODY);

                            ThreadedConversations threadedConversations = new ThreadedConversations();
                            String date = conversationsCursor.getString(dateIndex);
                            String address = conversationsCursor.getString(addressIndex);
                            String threadId = conversationsCursor.getString(threadIdIndex);
                            int type = conversationsCursor.getInt(typeIndex);
                            boolean read = conversationsCursor.getInt(readIndex) == 1;

                            threadedConversations.setAddress(address);
                            threadedConversations.setDate(date);
                            threadedConversations.setType(type);
                            threadedConversations.setIs_read(read);
                            threadedConversations.setThread_id(threadId);
                            threadedConversations.setSnippet(conversationsCursor
                                    .getString(bodyIndex));

                            if(BlockedNumberContract.isBlocked(context, threadedConversations.getAddress()))
                                threadedConversations.setIs_blocked(true);

                            String contactName = Contacts.retrieveContactName(context,
                                    threadedConversations.getAddress());
                            threadedConversations.setContact_name(contactName);
                            threadedConversationsList.add(threadedConversations);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } while(conversationsCursor.moveToNext());
                    databaseConnector.threadedConversationsDao()
                            .insertAll(threadedConversationsList);
                    conversationsCursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void refresh(Context context) {
//        List<ThreadedConversations> newThreadedConversationsList = new ArrayList<>();
//        Cursor cursor = context.getContentResolver().query(
//                Telephony.Threads.CONTENT_URI,
//                null,
//                null,
//                null,
//                "date DESC"
//        );
//
//        List<ThreadedConversations> threadedDraftsList =
//                databaseConnector.threadedConversationsDao().getThreadedDraftsList(
//                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
//
//        List<String> archivedThreads = databaseConnector.threadedConversationsDao().getArchivedList();
//        List<String> threadsIdsInDrafts = new ArrayList<>();
//        for(ThreadedConversations threadedConversations : threadedDraftsList)
//            threadsIdsInDrafts.add(threadedConversations.getThread_id());
//
//                /*
//                [date, rr, sub, subject, ct_t, read_status, reply_path_present, body, type, msg_box,
//                thread_id, sub_cs, resp_st, retr_st, text_only, locked, exp, m_id, retr_txt_cs, st,
//                date_sent, read, ct_cls, m_size, rpt_a, address, sub_id, pri, tr_id, resp_txt, ct_l,
//                m_cls, d_rpt, v, person, service_center, error_code, _id, m_type, status]
//                 */
//        List<ThreadedConversations> threadedConversationsList =
//                databaseConnector.threadedConversationsDao().getAll();
//        if(cursor != null && cursor.moveToFirst()) {
//            do {
//                ThreadedConversations threadedConversations = new ThreadedConversations();
//                int recipientIdIndex = cursor.getColumnIndex("address");
//                int snippetIndex = cursor.getColumnIndex("body");
//                int dateIndex = cursor.getColumnIndex("date");
//                int threadIdIndex = cursor.getColumnIndex("thread_id");
//                int typeIndex = cursor.getColumnIndex("type");
//                int readIndex = cursor.getColumnIndex("read");
//
//                threadedConversations.setIs_read(cursor.getInt(readIndex) == 1);
//
//                threadedConversations.setAddress(cursor.getString(recipientIdIndex));
//                if(threadedConversations.getAddress() == null || threadedConversations.getAddress().isEmpty())
//                    continue;
//                threadedConversations.setThread_id(cursor.getString(threadIdIndex));
//                if(threadsIdsInDrafts.contains(threadedConversations.getThread_id())) {
//                    ThreadedConversations tc = threadedDraftsList.get(
//                            threadsIdsInDrafts.indexOf(threadedConversations.getThread_id()));
//                    threadedConversations.setSnippet(tc.getSnippet());
//                    threadedConversations.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
//                    threadedConversations.setDate(
//                            Long.parseLong(tc.getDate()) >
//                                    Long.parseLong(cursor.getString(dateIndex)) ?
//                                    tc.getDate() : cursor.getString(dateIndex));
//                }
//                else {
//                    threadedConversations.setSnippet(cursor.getString(snippetIndex));
//                    threadedConversations.setType(cursor.getInt(typeIndex));
//                    threadedConversations.setDate(cursor.getString(dateIndex));
//                }
//                if(BlockedNumberContract.isBlocked(context, threadedConversations.getAddress()))
//                    threadedConversations.setIs_blocked(true);
//
//                threadedConversations.setIs_archived(
//                        archivedThreads.contains(threadedConversations.getThread_id()));
//
//                String contactName = Contacts.retrieveContactName(context,
//                        threadedConversations.getAddress());
//                threadedConversations.setContact_name(contactName);
//
//                /**
//                 * Check things that change first
//                 * - Read status
//                 * - Drafts
//                 */
//                if(!threadedConversationsList.contains(threadedConversations)) {
//                    newThreadedConversationsList.add(threadedConversations);
//                }
//            } while(cursor.moveToNext());
//            cursor.close();
//        }
//        databaseConnector.threadedConversationsDao().insertAll(newThreadedConversationsList);
//        getCount(context);
//    }

    public void unarchive(List<Archive> archiveList) {
        databaseConnector.threadedConversationsDao().unarchive(archiveList);
    }

    public void unblock(Context context, List<String> threadIds) {
        List<ThreadedConversations> threadedConversationsList =
                databaseConnector.threadedConversationsDao().getList(threadIds);
        for(ThreadedConversations threadedConversations : threadedConversationsList) {
            BlockedNumberContract.unblock(context, threadedConversations.getAddress());
        }
        refresh(context);
    }

    public void clearDrafts(Context context) {
        SMSDatabaseWrapper.deleteAllDraft(context);
        databaseConnector.threadedConversationsDao()
                .clearDrafts(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
        refresh(context);
    }

    public boolean hasUnread(List<String> ids) {
        return databaseConnector.threadedConversationsDao().getCountUnread(ids) > 0;
    }

    public void markUnRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 0, threadIds.toArray(new String[0]));
        databaseConnector.threadedConversationsDao().updateRead(0, threadIds);
        refresh(context);
    }

    public void markRead(Context context, List<String> threadIds) {
        NativeSMSDB.Incoming.update_all_read(context, 1, threadIds.toArray(new String[0]));
        databaseConnector.threadedConversationsDao().updateRead(1, threadIds);
        refresh(context);
    }

    public void markAllRead(Context context) {
        NativeSMSDB.Incoming.update_all_read(context, 1);
        databaseConnector.threadedConversationsDao().updateRead(1);
        refresh(context);
    }

    public MutableLiveData<List<Integer>> folderMetrics = new MutableLiveData<>();
    public void getCount(Context context) {
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
