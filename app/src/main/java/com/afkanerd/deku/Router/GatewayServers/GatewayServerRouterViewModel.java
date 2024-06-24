package com.afkanerd.deku.Router.GatewayServers;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkInfo;

import com.afkanerd.deku.Router.Models.RouterHandler;

import java.util.List;

public class GatewayServerRouterViewModel extends ViewModel {
    private LiveData<List<WorkInfo>> messagesList;

    public LiveData<List<WorkInfo>> getMessages(Context context){
        if(messagesList == null) {
            messagesList = loadSMSThreads(context);
        }
        return messagesList;
    }

    private LiveData<List<WorkInfo>> loadSMSThreads(Context context) {
        return RouterHandler.INSTANCE.getMessageIdsFromWorkManagers(context);
    }
}
