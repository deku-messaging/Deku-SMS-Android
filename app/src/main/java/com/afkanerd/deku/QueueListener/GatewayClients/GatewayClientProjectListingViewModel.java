package com.afkanerd.deku.QueueListener.GatewayClients;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;

import java.util.List;

public class GatewayClientProjectListingViewModel extends ViewModel {

    long id;
    MutableLiveData<List<GatewayClientProjects>> mutableLiveData = new MutableLiveData<>();
    public LiveData<List<GatewayClientProjects>> get(Context context, long id) {
        this.id = id;
        GatewayClientProjectDao gatewayClientProjectDao = Datastore.getDatastore(context)
                .gatewayClientProjectDao();
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                mutableLiveData.postValue(gatewayClientProjectDao.fetchGatewayClientIdList(id));
            }
        });
        return mutableLiveData;
    }

}
