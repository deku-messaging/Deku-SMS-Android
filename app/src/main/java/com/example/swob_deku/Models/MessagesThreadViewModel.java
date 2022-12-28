package com.example.swob_deku.Models;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.GatewayServer.GatewayServerDAO;

import java.util.ArrayList;
import java.util.List;

public class MessagesThreadViewModel extends ViewModel {
    private LiveData<List<SMS>> messagesList;

    public LiveData<List<SMS>> getMessages(Context context){
        loadSMSThreads(context);
        return messagesList;
    }

    private void loadSMSThreads(Context context) {
//        Cursor cursor = SMSHandler.fetchSMSMessagesThreads(context);
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);
        if(cursor.moveToFirst()) {
            List<SMS> smsList = new ArrayList<>();
            do {
                SMS sms = new SMS(cursor);
                smsList.add(sms);
            } while(cursor.moveToNext());
            messagesList = new MutableLiveData<>(smsList);
        }
    }
}
