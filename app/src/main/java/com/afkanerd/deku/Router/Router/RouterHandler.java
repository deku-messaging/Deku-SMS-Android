package com.afkanerd.deku.Router.Router;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_NAME;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_ROUTING_URL;

import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerDAO;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RouterHandler {
    public static final String TAG_WORKER_ID = "swob.work.id.";
    public static int MESSAGE_ID = 0;
    public static int WORK_NAME = 1;
    public static int ROUTING_URL = 2;
    public static int ROUTING_ID = 3;

    public static void routeJsonMessages(Context context, String jsonStringBody, String gatewayServerUrl)
            throws ExecutionException, InterruptedException, TimeoutException, JSONException {
        try{
            Log.d(RouterHandler.class.getName(), "Request to router: " + jsonStringBody);
            JSONObject jsonBody = new JSONObject(jsonStringBody);
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    gatewayServerUrl,
                    jsonBody, future, future);
            RequestQueue requestQueue = Volley.newRequestQueue(context);

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            requestQueue.add(jsonObjectRequest);
            future.get(30, TimeUnit.SECONDS);
        }
        catch (ExecutionException | TimeoutException | InterruptedException e){
            // Hit the server and came back with error code
            throw e;
        } // Because the server could return a string...

    }

    public static void route(Context context, RouterItem routerItem) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting().serializeNulls();
        Gson gson = gsonBuilder.create();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        boolean isBase64 = Helpers.isBase64Encoded(routerItem.getText());

        new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServer gatewayServer = new GatewayServer();
                GatewayServerDAO gatewayServerDAO = gatewayServer.getDaoInstance(context);
                List<GatewayServer> gatewayServerList = gatewayServerDAO.getAllList();
                gatewayServer.close();

                for (GatewayServer gatewayServer1 : gatewayServerList) {
                    if(gatewayServer1.getFormat() != null &&
                            gatewayServer1.getFormat().equals(GatewayServer.BASE64_FORMAT) && !isBase64)
                        continue;

                    routerItem.tag = gatewayServer1.getTag();
                    final String jsonStringBody = gson.toJson(routerItem);

                    try {
                        OneTimeWorkRequest routeMessageWorkRequest = new OneTimeWorkRequest.Builder(RouterWorkManager.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(TAG_NAME)
                                .addTag(getTagForMessages(routerItem.getMessage_id()))
                                .addTag(getTagForGatewayServers(gatewayServer1.getURL()))
                                .setInputData(
                                        new Data.Builder()
                                                .putString(RouterWorkManager.SMS_JSON_OBJECT, jsonStringBody)
                                                .putString(RouterWorkManager.SMS_JSON_ROUTING_URL, gatewayServer1.getURL())
                                                .build()
                                )
                                .build();

                        String uniqueWorkName = routerItem.getMessage_id() + ":" + gatewayServer1.getURL();
                        WorkManager workManager = WorkManager.getInstance(context);
                        workManager.enqueueUniqueWork(
                                uniqueWorkName,
                                ExistingWorkPolicy.KEEP,
                                routeMessageWorkRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private static String getTagForMessages(String messageId) {
        return TAG_WORKER_ID + messageId;
    }

    private static String getTagForGatewayServers(String gatewayClientUrl) {
        return TAG_ROUTING_URL + gatewayClientUrl;
    }

    public static void removeWorkForMessage(Context context, String messageId) {
        String tag = getTagForMessages(messageId);
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(tag);
    }

    public static void removeWorkForGatewayServers(Context context, String gatewayClientUrl) {
        String tag = getTagForGatewayServers(gatewayClientUrl);
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(tag);
    }

    public static ArrayList<String[]> getMessageIdsFromWorkManagers(Context context) {

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Collections.singletonList(
                        TAG_NAME))
                .addStates(Arrays.asList(
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.CANCELLED))
                .build();

        WorkManager workManager = WorkManager.getInstance(context);
        ListenableFuture<List<WorkInfo>> worksInfo = workManager.getWorkInfos(workQuery);

        ArrayList<String[]> workerIds = new ArrayList<>();
        try {
            List<WorkInfo> workInfoList = worksInfo.get();

            for(WorkInfo workInfo : workInfoList) {
                String messageId = "";
                String gatewayServerUrl = "";
                for(String tag : workInfo.getTags()) {
                    if (tag.contains(RouterHandler.TAG_WORKER_ID)) {
                        String[] tags = tag.split("\\.");
                        messageId = tags[tags.length - 1];
                    }
                    if (tag.contains(IncomingTextSMSBroadcastReceiver.TAG_ROUTING_URL)) {
                        String[] tags = tag.split(",");
                        gatewayServerUrl = tags[tags.length - 1];
                    }
                }

//                ArrayList<String> routeJobState = new ArrayList<>();
                String[] routeJobState = new String[4];
                if(!messageId.isEmpty() && !gatewayServerUrl.isEmpty()) {
                    routeJobState[MESSAGE_ID] = messageId;
                    routeJobState[WORK_NAME] = workInfo.getState().name();
                    routeJobState[ROUTING_URL] = gatewayServerUrl;
                    routeJobState[ROUTING_ID] = workInfo.getId().toString();
                }

                workerIds.add(routeJobState);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return workerIds;
    }
}
