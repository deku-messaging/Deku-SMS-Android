package com.example.swob_deku.Models.Router;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RouterViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;

    public LiveData<List<SMS>> getMessages(Context context){
        if(messagesList == null) {
            messagesList = new MutableLiveData<>();
            loadSMSThreads(context);
        }
        return messagesList;
    }

    public void informChanges(Context context) {
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) {
        ArrayList<ArrayList<String>> routerJobs = listRouteJobs(context);

        if(routerJobs.isEmpty())
            return;

        ArrayList<Long> workerIds = new ArrayList<>();

        for(ArrayList workerList : routerJobs)
            workerIds.add(Long.valueOf(workerList.get(0).toString()));

        Cursor cursor = SMSHandler.fetchSMSMessageForAllIds(context, workerIds);

        if(cursor.moveToFirst()) {
            List<SMS> smsList = new ArrayList<>();
            do {
                SMS sms = new SMS(cursor);
                smsList.add(sms);
            } while(cursor.moveToNext());

            for(int j = 0; j < smsList.size(); ++j) {
                SMS sms = smsList.get(j);
                for(int i = 0;i< workerIds.size(); ++i ) {
                    if(workerIds.get(i).equals(Long.valueOf(sms.getId()))) {
                        smsList.get(j).setRouterStatus(routerJobs.get(i).get(1));
                        break;
                    }
                }
                for(int i = 0;i< workerIds.size(); ++i ) {
                    if(workerIds.get(i).equals(Long.valueOf(sms.getId()))) {
                        if(routerJobs.get(i).size() > 2)
                            smsList.get(j).addRoutingUrl(routerJobs.get(i).get(2));
                    }
                }
            }
            messagesList.setValue(smsList);
        }
        cursor.close();
    }

    public ArrayList<ArrayList<String>> listRouteJobs(Context context) {

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Arrays.asList(
                        IncomingTextSMSBroadcastReceiver.TAG_NAME))
                .addStates(Arrays.asList(
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.CANCELLED))
                .build();

        WorkManager workManager = WorkManager.getInstance(context);
        ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfos(workQuery);

        ArrayList<ArrayList<String>> workerIds = new ArrayList<>();
        try {
            List<WorkInfo> workInfoList = workInfos.get();

            String messageId = "";
            String gatewayServerUrl = "";
            for(WorkInfo workInfo : workInfoList) {
                String[] Alltags = Helpers.convertSetToStringArray(workInfo.getTags());
                for(int i = 0; i< Alltags.length; ++i) {
                    if (Alltags[i].contains(IncomingTextSMSBroadcastReceiver.TAG_WORKER_ID)) {
                        String[] tags = Alltags[i].split("\\.");
                        messageId = tags[tags.length - 1];
                    }
                    if (Alltags[i].contains(IncomingTextSMSBroadcastReceiver.TAG_ROUTING_URL)) {
                        String[] tags = Alltags[i].split(",");
                        gatewayServerUrl = tags[tags.length - 1];
                    }
                }

                ArrayList<String> routeJobState = new ArrayList<>();
                if(!messageId.isEmpty()) {
                    routeJobState.add(messageId);
                    routeJobState.add(workInfo.getState().name());
                }
                if(!gatewayServerUrl.isEmpty()) {
                    routeJobState.add(gatewayServerUrl);
                }
                if(!routeJobState.isEmpty())
                    workerIds.add(routeJobState);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return workerIds;
    }
}
