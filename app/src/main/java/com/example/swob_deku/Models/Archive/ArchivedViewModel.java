package com.example.swob_deku.Models.Archive;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class ArchivedViewModel extends ViewModel {

    MutableLiveData<List<SMS>> liveData;
    Context context;
    public LiveData<List<SMS>> getMessages(Context context) throws InterruptedException {
        this.context = context;
        if(liveData == null) {
            liveData = new MutableLiveData<>();
            loadMessages();
        }
        return liveData;
    }

    public void loadMessages() throws InterruptedException {
        List<Archive> archiveList = ArchiveHandler.loadAllMessages(context);
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);

        List<SMS> smsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                SMS sms = new SMS(cursor);
                for(Archive archive : archiveList)
                    if(Long.parseLong(sms.getThreadId()) == archive.getThreadId()) {
                        smsList.add(sms);
                        break;
                    }
            } while(cursor.moveToNext());
        }
        liveData.setValue(smsList);
        cursor.close();
    }
}
