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

    long id;
    public LiveData<List<GatewayClientProjects>> get(Datastore databaseConnector, long id) {
        this.id = id;
        GatewayClientProjectDao gatewayClientProjectDao = databaseConnector.gatewayClientProjectDao();
        return gatewayClientProjectDao.fetchGatewayClientId(id);
    }

}
