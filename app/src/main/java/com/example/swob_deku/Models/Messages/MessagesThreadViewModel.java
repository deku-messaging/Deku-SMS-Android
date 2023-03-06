package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class MessagesThreadViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;

    public LiveData<List<SMS>> getMessages(Context context){
        if(messagesList == null) {
            messagesList = new MutableLiveData<>();
            loadSMSThreads(context);
        }
        return messagesList;
    }

    public void informChanges(Context context) {
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) {
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
        if(cursor.moveToFirst()) {
            List<SMS> smsList = new ArrayList<>();
            do {
                SMS sms = new SMS(cursor);
                smsList.add(sms);
                messagesList.postValue(smsList);
            } while(cursor.moveToNext());

            // Because meain thread
//            messagesList.setValue(smsList);

            // Because background thread
        }
        cursor.close();
    }
}
