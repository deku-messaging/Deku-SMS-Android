package com.afkanerd.deku.QueueListener.GatewayClients;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GatewayClientProjectListingViewModel extends ViewModel {

    Datastore databaseConnector;
    public LiveData<List<GatewayClientProjects>> get(Context context, long id) {
        Log.d(getClass().getName(), "Fetching Gateway Projects: " + id);
        databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .enableMultiInstanceInvalidation()
                .build();
        GatewayClientProjectDao gatewayClientProjectDao = databaseConnector.gatewayClientProjectDao();
        return gatewayClientProjectDao.fetchGatewayClientId(id);
    }

}
