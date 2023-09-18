package com.example.swob_deku.Models.GatewayServers;


import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class GatewayServerViewModel extends ViewModel {
    private LiveData<List<GatewayServer>> gatewayServersList;

    public LiveData<List<GatewayServer>> get(Context context) throws InterruptedException {
        if(gatewayServersList == null) {
            gatewayServersList = new MutableLiveData<>();
            loadGatewayServers(context);
        }
        return gatewayServersList;
    }

    private void loadGatewayServers(Context context) throws InterruptedException {
        GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(context);
        gatewayServersList = gatewayServerHandler.getAllLiveData();
    }
}
