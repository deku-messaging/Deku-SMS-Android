package com.afkanerd.deku.QueueListener.GatewayClients;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GatewayClientProjectListingViewModel extends ViewModel {

    MutableLiveData<List<GatewayClientProjects>> mutableLiveData = new MutableLiveData<>();
    public LiveData<List<GatewayClientProjects>> get(Context context, long id) {
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<GatewayClientProjects> gatewayClientProjects = new ArrayList<>();
                for(GatewayClient gatewayClient : fetchFilter(gatewayClientHandler, id)) {
                    GatewayClientProjects gatewayClientProject = new GatewayClientProjects();
                    gatewayClientProject.name = gatewayClient.getProjectName();
                    gatewayClientProject.binding1Name = gatewayClient.getProjectBinding();
                    gatewayClientProject.binding2Name = gatewayClient.getProjectBinding2();
                    gatewayClientProjects.add(gatewayClientProject);
                }
                mutableLiveData.postValue(gatewayClientProjects);
            }
        }).start();
        return mutableLiveData;
    }

    private List<GatewayClient> fetchFilter(GatewayClientHandler gatewayClientHandler, long id) {

        List<GatewayClient> filterGatewayClients = new ArrayList<>();
        try {
            List<GatewayClient> gatewayClientList = gatewayClientHandler.fetchAll();
            for(GatewayClient gatewayClient : gatewayClientList) {
                boolean contained = false;
                for(GatewayClient gatewayClient1 : filterGatewayClients) {
                    if(gatewayClient1.same(gatewayClient)) {
                        contained = true;
                        break;
                    }
                }
                if(!contained)
                    filterGatewayClients.add(gatewayClient);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return filterGatewayClients;
    }
}
