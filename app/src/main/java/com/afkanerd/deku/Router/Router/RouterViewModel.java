package com.afkanerd.deku.Router.Router;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkInfo;

import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouterViewModel extends ViewModel {
//    private MutableLiveData<List<RouterItem>> messagesList;
    private LiveData<List<WorkInfo>> messagesList;

    public LiveData<List<WorkInfo>> getMessages(Context context){
        if(messagesList == null) {
            messagesList = loadSMSThreads(context);
        }
        return messagesList;
    }

    private LiveData<List<WorkInfo>> loadSMSThreads(Context context) {
        return RouterHandler.getMessageIdsFromWorkManagers(context);

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                ListMultimap<Long, RouterItem> routerMessagesListMultimap = ArrayListMultimap.create();
//
//                for(String[] workerList : routerJobs) {
//                    String messageId = workerList[0];
//                    Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
//                    if(cursor.moveToFirst()) {
//                        int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
//                        int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
//                        int dateTimeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
//                        int bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY);
//
//                        String threadId = cursor.getString(threadIdIndex);
//                        String address = cursor.getString(addressIndex);
//                        String body = cursor.getString(bodyIndex);
//                        String date = cursor.getString(dateTimeIndex);
//
//                        String routerStatus = workerList[1];
//                        String url = workerList[2];
//                        RouterItem routerMessage = new RouterItem(cursor);
//                        cursor.close();
//
//                        routerMessage.routingStatus = routerStatus;
//                        routerMessage.url = url;
//                        routerMessage.routingDate = Long.parseLong(date);
//                        routerMessage.setMessage_id(messageId);
//                        routerMessage.setText(body);
//                        routerMessage.setAddress(address);
//
//                        routerMessagesListMultimap.put(Long.parseLong(date), routerMessage);
//                    }
//                }
//                List<RouterItem> sortedList = new ArrayList<>();
//                List<Long> keys = new ArrayList<>(routerMessagesListMultimap.keySet());
//                keys.sort(Collections.reverseOrder());
//                for(Long date : keys) {
//                    sortedList.addAll(routerMessagesListMultimap.get(date));
//                }
//
//                messagesList.postValue(sortedList);
//            }
//        });
//        thread.start();
//

    }
}
