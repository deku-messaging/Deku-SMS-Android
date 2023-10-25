package com.afkanerd.deku.DefaultSMS.Models.Archive;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class ArchivedViewModel extends ViewModel {

//    MutableLiveData<List<SMS>> liveData;
    MutableLiveData<List<Conversations>> liveData;
    Context context;

    ArchiveHandler archiveHandler;
    public LiveData<List<Conversations>> getMessages(Context context) throws InterruptedException {
        this.context = context;
        if(liveData == null) {
            liveData = new MutableLiveData<>();
            archiveHandler = new ArchiveHandler(context);
            loadMessages();
        }
        return liveData;
    }

    public void informChanges() throws InterruptedException {
        loadMessages();
    }

    private void loadMessages() throws InterruptedException {
        List<Archive> archiveList = archiveHandler.loadAllMessages(context);
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);

        List<Conversations> smsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                Conversations conversations = new Conversations(cursor);
                for(Archive archive : archiveList)
                    if(Long.parseLong(conversations.THREAD_ID) == archive.getThreadId()) {
                        conversations.setNewestMessage(context);
                        smsList.add(conversations);
                        break;
                    }
            } while(cursor.moveToNext());
        }
        cursor.close();
        liveData.setValue(smsList);
    }
}
