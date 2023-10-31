package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment;
import com.afkanerd.deku.DefaultSMS.Models.Archive.ArchiveHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConversationsThreadViewModel extends ViewModel {
    private MutableLiveData<List<ThreadedConversations>> conversationsMutableLiveData = new MutableLiveData<>();
    public LiveData<List<ThreadedConversations>> conversationsLiveData;
    String messagesType;

    ThreadedConversationsDao threadedConversationsDao;
    public LiveData<List<ThreadedConversations>> get(ThreadedConversationsDao threadedConversationsDao, Context context) {
        this.threadedConversationsDao = threadedConversationsDao;
        this.conversationsLiveData = conversationsMutableLiveData;

        loadNative(context);
        return this.conversationsLiveData;
    }

    private void loadNative(Context context){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = SMSHandler.fetchSMSForThreading(context);
                List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                if(cursor.moveToNext()) {
                    do {
                        threadedConversationsList.add(ThreadedConversations.build(cursor));
                    } while(cursor.moveToNext());
                }
                cursor.close();
                threadedConversationsDao.insert(threadedConversationsList);
                conversationsLiveData = threadedConversationsDao.getAllWithoutArchived();
            }
        }).start();
    }

//    public LiveData<List<Conversations>> getMessages(Context context, String messagesType) throws GeneralSecurityException, IOException {
//        if(conversationsMutableLiveData == null) {
//            this.messagesType = messagesType;
//            conversationsMutableLiveData = new MutableLiveData<>();
//            loadSMSThreads(context);
//        }
//        return conversationsMutableLiveData;
//    }
//
//    public void informChanges(Context context) throws GeneralSecurityException, IOException {
//        Log.d(getClass().getName(), "Running for informing changes");
//        loadSMSThreads(context);
//    }
//
//    private void loadSMSThreads(Context context) throws GeneralSecurityException, IOException {
//        ArchiveHandler archiveHandler = new ArchiveHandler(context);
//        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
//
//        switch (messagesType) {
//            case ThreadedConversationsFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT: {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
//                        try {
//                            SecurityECDH securityECDH = new SecurityECDH(context);
//                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
//                            if (cursor.moveToFirst()) {
//                                do {
//                                    Conversations conversation = new Conversations(cursor);
//                                    try {
//                                        if(!encryptedContacts.containsKey(conversation.THREAD_ID) ||
//                                                archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
//                                            continue;
//                                        }
//                                        conversation.setNewestMessage(context);
//                                        long date = conversation.getNewestMessage().getNewestDateTime();
//                                        conversationsListMultimap.put(date, conversation);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                } while (cursor.moveToNext());
//                                cursor.close();
//                            }
//                        } catch (GeneralSecurityException | IOException e) {
//                            e.printStackTrace();
//                        }
//                        List<Conversations> sortedList = new ArrayList<>();
//                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
//                        keys.sort(Collections.reverseOrder());
//                        for(Long date : keys) {
//                            sortedList.addAll(conversationsListMultimap.get(date));
//                        }
//                        conversationsMutableLiveData.postValue(sortedList);
//                        archiveHandler.close();
//                    }
//                }).start();
//                break;
//            }
//
//            case ThreadedConversationsFragment.ALL_MESSAGES_THREAD_FRAGMENT: {
//                Thread thread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
//                        if (cursor.moveToFirst()) {
//                            do {
//                                Conversations conversation = new Conversations(cursor);
//                                try {
//                                    if(archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
//                                        continue;
//                                    }
//                                    conversation.setNewestMessage(context);
//                                    long date = conversation.getNewestMessage().getNewestDateTime();
//                                    conversationsListMultimap.put(date, conversation);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            } while (cursor.moveToNext());
//                            cursor.close();
//                        }
//                        List<Conversations> sortedList = new ArrayList<>();
//                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
//                        keys.sort(Collections.reverseOrder());
//                        for(Long date : keys) {
//                            sortedList.addAll(conversationsListMultimap.get(date));
//                        }
//
//                        conversationsMutableLiveData.postValue(sortedList);
//                        archiveHandler.close();
//                    }
//
//                });
//                thread.start();
//                break;
//            }
//
//            case ThreadedConversationsFragment.PLAIN_MESSAGES_THREAD_FRAGMENT: {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
//                        try {
//                            SecurityECDH securityECDH = new SecurityECDH(context);
//                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
//                            if (cursor.moveToFirst()) {
//                                do {
//                                    Conversations conversation = new Conversations(cursor);
//                                    try {
//                                        if(encryptedContacts.containsKey(conversation.THREAD_ID) ||
//                                                archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
//                                            continue;
//                                        }
//                                        conversation.setNewestMessage(context);
//                                        long date = conversation.getNewestMessage().getNewestDateTime();
//                                        conversationsListMultimap.put(date, conversation);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                } while (cursor.moveToNext());
//                                cursor.close();
//                            }
//                        } catch (GeneralSecurityException | IOException e) {
//                            e.printStackTrace();
//                        }
//                        List<Conversations> sortedList = new ArrayList<>();
//                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
//                        keys.sort(Collections.reverseOrder());
//                        for(Long date : keys) {
//                            sortedList.addAll(conversationsListMultimap.get(date));
//                        }
//                        conversationsMutableLiveData.postValue(sortedList);
//                        archiveHandler.close();
//                    }
//                }).start();
//                break;
//            }
//
//        }
//    }
}
