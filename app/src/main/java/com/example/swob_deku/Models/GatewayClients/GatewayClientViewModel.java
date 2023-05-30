package com.example.swob_deku.Models.GatewayClients;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class GatewayClientViewModel extends ViewModel {

    LiveData<List<GatewayClient>> gatewayClientList;

    public LiveData<List<GatewayClient>> getGatewayClientList(GatewayClientDAO gatewayClientDAO) {
        if(gatewayClientList == null) {
            gatewayClientList = new MutableLiveData<>();
            loadGatewayClients(gatewayClientDAO);
        }
        return gatewayClientList;
    }

    public void loadGatewayClients(GatewayClientDAO gatewayClientDAO) {
        gatewayClientList = gatewayClientDAO.getAll();
    }
}
