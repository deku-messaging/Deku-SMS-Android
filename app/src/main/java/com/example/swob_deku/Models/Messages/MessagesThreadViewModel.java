package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Fragments.Homepage.MessagesThreadFragment;
import com.example.swob_deku.MessagesThreadsActivity;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class MessagesThreadViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;
    private LiveData<List<SMS>> messagesListLiveData;

    String messagesType;

    public LiveData<List<SMS>> getMessages(Context context, String messagesType) throws GeneralSecurityException, IOException {
        if(messagesListLiveData == null) {
            this.messagesType = messagesType;

            messagesList = new MutableLiveData<>();
            messagesListLiveData = messagesList;

            loadSMSThreads(context);
        }
        return messagesListLiveData;
    }

    public void informChanges(Context context) throws GeneralSecurityException, IOException {
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) {
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
        List<SMS> smsList = new ArrayList<>();
        ArchiveHandler archiveHandler = new ArchiveHandler(context);

        switch (messagesType) {
            case MessagesThreadFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (cursor.moveToFirst()) {
                            do {
                                SMS sms = new SMS(cursor);
                                SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
                                smsMetaEntity.setAddress(context, sms.getAddress());
                                try {
                                    if (!smsMetaEntity.isEncrypted(context))
                                        continue;
                                    else {
                                        if (archiveHandler.isArchived(Long.parseLong(sms.getThreadId()))) {
                                            continue;
                                        }
                                    }
                                } catch (GeneralSecurityException | IOException |
                                         InterruptedException e) {
                                    e.printStackTrace();
                                }
                                smsList.add(sms);
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                        archiveHandler.close();
                        messagesList.postValue(smsList);
                    }
                }).start();
                break;
            }
            case MessagesThreadFragment.ALL_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (cursor.moveToFirst()) {
                            do {
                                SMS sms = new SMS(cursor);
                                try {
                                    if (archiveHandler.isArchived(Long.parseLong(sms.getThreadId()))) {
                                        continue;
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                smsList.add(sms);
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                        archiveHandler.close();
                        messagesList.postValue(smsList);
                    }
                }).start();
                break;
            }
            case MessagesThreadFragment.PLAIN_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (cursor.moveToFirst()) {
                            do {
                                SMS sms = new SMS(cursor);
                                SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
                                smsMetaEntity.setAddress(context, sms.getAddress());
                                try {
                                    if (smsMetaEntity.isEncrypted(context))
                                        continue;
                                    else {
                                        if (archiveHandler.isArchived(Long.parseLong(sms.getThreadId()))) {
                                            continue;
                                        }
                                    }
                                } catch (GeneralSecurityException | IOException |
                                         InterruptedException e) {
                                    e.printStackTrace();
                                }
                                smsList.add(sms);
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                        archiveHandler.close();
                        messagesList.postValue(smsList);
                    }
                }).start();
                break;
            }
        }
    }
}
