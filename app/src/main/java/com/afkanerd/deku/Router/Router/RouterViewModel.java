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
    private LiveData<List<WorkInfo>> messagesList;

    public LiveData<List<WorkInfo>> getMessages(Context context){
        if(messagesList == null) {
            messagesList = loadSMSThreads(context);
        }
        return messagesList;
    }

    private LiveData<List<WorkInfo>> loadSMSThreads(Context context) {
        return RouterHandler.getMessageIdsFromWorkManagers(context);
    }
}
