package com.afkanerd.deku.QueueListener.GatewayClients;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class GatewayClientViewModel extends ViewModel {

    private MutableLiveData<List<GatewayClient>> gatewayClientList;
    GatewayClientDAO gatewayClientDAO;

    public MutableLiveData<List<GatewayClient>> getGatewayClientList(Context context, GatewayClientDAO gatewayClientDAO) {
        if(gatewayClientList == null) {
            this.gatewayClientDAO = gatewayClientDAO;
            gatewayClientList = new MutableLiveData<>();
            loadGatewayClients(context);
        }
        return gatewayClientList;
    }

    public void refresh(Context context) {
        loadGatewayClients(context);
    }

    private void loadGatewayClients(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<GatewayClient> gatewayClients = normalizeGatewayClients(gatewayClientDAO.getAll());
                for(GatewayClient gatewayClient : gatewayClients)
                    gatewayClient.setConnectionStatus(
                            GatewayClientHandler.getConnectionStatus(context,
                                    String.valueOf(gatewayClient.getId())));

                gatewayClientList.postValue(gatewayClients);
            }
        }).start();
    }

    private List<GatewayClient> normalizeGatewayClients(List<GatewayClient> gatewayClients) {
        List<GatewayClient> filteredGatewayClients = new ArrayList<>();
        for(GatewayClient gatewayClient : gatewayClients) {
            boolean contained = false;
            for(GatewayClient gatewayClient1 : filteredGatewayClients) {
                if(gatewayClient1.same(gatewayClient)) {
                    contained = true;
                    break;
                }
            }
            if(!contained) {
                filteredGatewayClients.add(gatewayClient);
            }
        }

        return filteredGatewayClients;
    }

}
