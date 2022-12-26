package com.example.swob_deku.Models.GatewayServer;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class GatewayServerViewModel extends ViewModel {
    private LiveData<List<GatewayServer>> gatewayServersList;

    public LiveData<List<GatewayServer>> getGatewayServers(GatewayServerDAO gatewayServerDAO, LiveData<List<GatewayServer>> gatewayServerLiveData){
        this.gatewayServersList = gatewayServerLiveData;
        loadGatewayServers(gatewayServerDAO);
        return gatewayServersList;
    }

    private void loadGatewayServers(GatewayServerDAO gatewayServerDAO) {
        gatewayServersList = gatewayServerDAO.getAll();
    }
}
