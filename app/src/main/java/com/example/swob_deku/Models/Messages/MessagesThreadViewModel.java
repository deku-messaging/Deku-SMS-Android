package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class MessagesThreadViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;
    private LiveData<List<SMS>> messagesListLiveData;

    public LiveData<List<SMS>> getMessages(Context context){
        if(messagesListLiveData == null) {
            messagesList = new MutableLiveData<>();
            messagesListLiveData = messagesList;
            loadSMSThreads(context);
        }
        return messagesListLiveData;
    }

    public void informChanges(Context context) {
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) {
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
        List<SMS> smsList = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveHandler archiveHandler = new ArchiveHandler(context);
                if(cursor.moveToFirst()) {
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
    }
}
