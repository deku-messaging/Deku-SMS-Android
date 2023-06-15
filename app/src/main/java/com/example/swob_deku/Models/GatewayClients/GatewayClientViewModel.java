package com.example.swob_deku.Models.GatewayClients;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
                List<GatewayClient> gatewayClients = gatewayClientDAO.getAll();
                Log.d(getClass().getName(), "Number of items: " + gatewayClients.size());

                if(gatewayClients != null)
                    for(GatewayClient gatewayClient : gatewayClients)
                        gatewayClient.setConnectionStatus(
                                GatewayClientHandler.getConnectionStatus(context,
                                        String.valueOf(gatewayClient.getId())));

                gatewayClientList.postValue(gatewayClients);
            }
        }).start();
    }
}
