package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationsSearchViewModel extends ViewModel {
    private MutableLiveData<List<Conversations>> messagesList;

    public LiveData<List<Conversations>> getMessages(Context context, String searchInput){
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

        List<Conversations> smsList = new ArrayList<>();
        Map<String, String> stringStringMap = new HashMap<>();

        if(cursor.moveToFirst()) {
            do {
                int bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY);
                int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);

                String snippets = cursor.getString(bodyIndex);
                String threadId = cursor.getString(threadIdIndex);
//                if(stringStringMap.containsKey(threadId))
//                    stringStringMap.replace(threadId, snippets);
//                else
//                    stringStringMap.put(threadId, snippets);
                stringStringMap.put(threadId, snippets);
            } while(cursor.moveToNext());

            for(Map.Entry<String, String> entry : stringStringMap.entrySet()) {
                Conversations conversations = new Conversations();
                conversations.setTHREAD_ID(entry.getKey());
                conversations.setSNIPPET(entry.getValue());
                conversations.setNewestMessage(context);
                smsList.add(conversations);
            }
        }
        cursor.close();
        messagesList.setValue(smsList);
    }
}
