package com.example.swob_deku.Models.Router;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RouterViewModel extends ViewModel {
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
        ArrayList<ArrayList<String>> routerJobs = RouterHandler.getMessageIdsFromWorkManagers(context);

        if(routerJobs.isEmpty())
            return;

        List<SMS> smsList = new ArrayList<>();
        for(ArrayList<String> workerList : routerJobs) {
//            long messageId = Long.parseLong(workerList.get(0));
//            Cursor cursor = SMSHandler.fetchSMSMessageForAllIds(context, workerIds);
            String messageId = workerList.get(0);
            Cursor cursor = SMSHandler.fetchSMSInboxById(context, messageId);
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                sms.setRouterStatus(workerList.get(1));
                sms.addRoutingUrl(workerList.get(2));
                smsList.add(sms);
            }
            cursor.close();
        }
        messagesList.setValue(smsList);
    }
}
