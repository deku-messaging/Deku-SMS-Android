package com.example.swob_deku.Models.GatewayClients;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class GatewayClientViewModel extends ViewModel {

    private LiveData<List<GatewayClient>> gatewayClientList;
    GatewayClientDAO gatewayClientDAO;

    public LiveData<List<GatewayClient>> getGatewayClientList(GatewayClientDAO gatewayClientDAO) {
        if(gatewayClientList == null) {
            this.gatewayClientDAO = gatewayClientDAO;
            gatewayClientList = new MutableLiveData<>();
            loadGatewayClients();
        }
        return gatewayClientList;
    }

    public void refresh() {
        loadGatewayClients();
    }

    private void loadGatewayClients() {
        gatewayClientList = gatewayClientDAO.getAll();
    }
}
