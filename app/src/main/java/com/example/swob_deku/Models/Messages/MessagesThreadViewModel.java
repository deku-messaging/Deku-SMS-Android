package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Fragments.Homepage.MessagesThreadFragment;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.SMS.Conversations;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MessagesThreadViewModel extends ViewModel {
    private MutableLiveData<List<Conversations>> messagesList;
    private LiveData<List<SMS>> messagesListLiveData;

    private MutableLiveData<List<Conversations>> conversationsMutableLiveData;
    private LiveData<List<Conversations>> conversationsLiveData;

    String messagesType;

    public MutableLiveData<List<Conversations>> getMessages(Context context, String messagesType) throws GeneralSecurityException, IOException {
        Log.d(getClass().getName(), "Running for getting messages!");
        if(messagesListLiveData == null) {
            this.messagesType = messagesType;

            conversationsMutableLiveData = new MutableLiveData<>();

            loadSMSThreads(context);
        }
        return conversationsMutableLiveData;
    }

    public void informChanges(Context context) throws GeneralSecurityException, IOException {
        Log.d(getClass().getName(), "Running for informing changes");
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) throws GeneralSecurityException, IOException {
        Log.d(getClass().getName(), "Running for loading all the threads");
        ArchiveHandler archiveHandler = new ArchiveHandler(context);

        switch (messagesType) {
            case MessagesThreadFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TreeMap<Long, Conversations> conversationsHashMap = new TreeMap<>(Collections.reverseOrder());
                        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
                        List<Conversations> conversations = new ArrayList<>();
                        try {
                            SecurityECDH securityECDH = new SecurityECDH(context);
                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
                            if (cursor.moveToFirst()) {
                                do {
                                    Conversations conversation = new Conversations(cursor);
                                    conversation.setNewestMessage(context);
                                    SMS.SMSMetaEntity smsMetaEntity = conversation.getNewestMessage(context);
                                    try {
                                        if (encryptedContacts.containsKey(smsMetaEntity.getAddress()) &&
                                                !archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID)) )
                                            conversationsHashMap.put(smsMetaEntity.getNewestDateTime(), conversation);
                                    } catch ( InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } while (cursor.moveToNext());
                                if(conversationsHashMap.isEmpty())
                                    conversationsMutableLiveData.postValue(conversations);
                                else
                                    for(Map.Entry<Long, Conversations> entry : conversationsHashMap.entrySet()) {
                                        conversations.add(entry.getValue());
                                        conversationsMutableLiveData.postValue(conversations);
                                    }
                            }
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                        cursor.close();
                        archiveHandler.close();
                    }
                }).start();
                break;
            }

            case MessagesThreadFragment.ALL_MESSAGES_THREAD_FRAGMENT: {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
                        List<Conversations> conversations = new ArrayList<>();
                        if (cursor.moveToFirst()) {
                            do {
                                Conversations conversation = new Conversations(cursor);
                                try {
                                    if(archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
                                        continue;
                                    }
                                    conversations.add(conversation);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while (cursor.moveToNext());
                            cursor.close();
                        }
                        conversationsMutableLiveData.postValue(conversations);
                        archiveHandler.close();
                    }

                });
                thread.start();

                try {
                    thread.join();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            case MessagesThreadFragment.PLAIN_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TreeMap<Long, Conversations> conversationsHashMap = new TreeMap<>(Collections.reverseOrder());
                        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
                        List<Conversations> conversations = new ArrayList<>();
                        try {
                            SecurityECDH securityECDH = new SecurityECDH(context);
                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
                            if (cursor.moveToFirst()) {
                                do {
                                    Conversations conversation = new Conversations(cursor);
                                    conversation.setNewestMessage(context);
                                    SMS.SMSMetaEntity smsMetaEntity = conversation.getNewestMessage(context);
                                    try {
                                        if (!encryptedContacts.containsKey(smsMetaEntity.getAddress()) &&
                                                !archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID)) )
                                            conversationsHashMap.put(smsMetaEntity.getNewestDateTime(), conversation);
                                    } catch ( InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } while (cursor.moveToNext());
                                if(conversationsHashMap.isEmpty())
                                    conversationsMutableLiveData.postValue(conversations);
                                else
                                    for(Map.Entry<Long, Conversations> entry : conversationsHashMap.entrySet()) {
                                        conversations.add(entry.getValue());
                                        conversationsMutableLiveData.postValue(conversations);
                                    }
                            }
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                        cursor.close();
                        archiveHandler.close();
                    }
                }).start();
                break;
            }

        }
    }
}
