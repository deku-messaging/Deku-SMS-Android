package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class MessagesSearchViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;

    public LiveData<List<SMS>> getMessages(Context context, String searchInput){
        if(messagesList == null) {
            messagesList = new MutableLiveData<>();
            loadSMSThreads(context, searchInput);
        }
        return messagesList;
    }

    public void informChanges(Context context, String searchInput) {
        loadSMSThreads(context, searchInput);
    }

    private void loadSMSThreads(Context context, String searchInput) {
        if(searchInput == null || searchInput.isEmpty())
            return;
        Cursor cursor = SMSHandler.fetchSMSMessagesForSearch(context, searchInput);

        List<SMS> smsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                SMS sms = new SMS(cursor);
                smsList.add(sms);
            } while(cursor.moveToNext());
        }
        cursor.close();
        messagesList.setValue(smsList);
    }
}
