package com.afkanerd.deku.Router.Router;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouterViewModel extends ViewModel {
    private MutableLiveData<List<RouterMessages>> messagesList;

    public LiveData<List<RouterMessages>> getMessages(Context context){
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
        ArrayList<String[]> routerJobs = RouterHandler.getMessageIdsFromWorkManagers(context);

        if(routerJobs.isEmpty())
            return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ListMultimap<Long, RouterMessages> routerMessagesListMultimap = ArrayListMultimap.create();

                for(String[] workerList : routerJobs) {
                    String messageId = workerList[0];
                    Cursor cursor = SMSHandler.fetchSMSInboxById(context, messageId);
                    if(cursor.moveToFirst()) {
                        int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
                        int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
                        int dateTimeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
                        int bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY);

                        String threadId = cursor.getString(threadIdIndex);
                        String address = cursor.getString(addressIndex);
                        String body = cursor.getString(bodyIndex);
                        String date = cursor.getString(dateTimeIndex);
                        cursor.close();

                        String routerStatus = workerList[1];
                        String url = workerList[2];
                        RouterMessages routerMessage = new RouterMessages();

                        routerMessage.setId(workerList[3]);
                        routerMessage.setThreadId(threadId);
                        routerMessage.setStatus(routerStatus);
                        routerMessage.setUrl(url);
                        routerMessage.setDate(Long.parseLong(date));
                        routerMessage.setMessageId(Long.parseLong(messageId));
                        routerMessage.setBody(body);
                        routerMessage.setAddress(address);

                        routerMessagesListMultimap.put(Long.parseLong(date), routerMessage);
                    }
                }
                List<RouterMessages> sortedList = new ArrayList<>();
                List<Long> keys = new ArrayList<>(routerMessagesListMultimap.keySet());
                keys.sort(Collections.reverseOrder());
                for(Long date : keys) {
                    sortedList.addAll(routerMessagesListMultimap.get(date));
                }

                messagesList.postValue(sortedList);
            }
        });
        thread.start();
    }
}
