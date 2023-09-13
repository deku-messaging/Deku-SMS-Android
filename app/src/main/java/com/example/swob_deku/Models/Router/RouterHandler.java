package com.example.swob_deku.Models.Router;

import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_NAME;
import static com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_ROUTING_URL;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServers.GatewayServer;
import com.example.swob_deku.Models.GatewayServers.GatewayServerDAO;
import com.example.swob_deku.Models.RMQ.RMQConnectionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RouterHandler {
    public static final String TAG_WORKER_ID = "swob.work.id.";

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

    public static void createWorkForMessage(Context context,
                                            RMQConnectionService.SmsForwardInterface jsonObject, long messageId,
                                            boolean isBase64) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting().serializeNulls();
        Gson gson = gsonBuilder.create();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                Datastore.databaseName).build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayServerDAO gatewayServerDAO = databaseConnector.gatewayServerDAO();
                List<GatewayServer> gatewayServerList = gatewayServerDAO.getAllList();

                for (GatewayServer gatewayServer : gatewayServerList) {
                    if(gatewayServer.getFormat() != null &&
                            gatewayServer.getFormat().equals(GatewayServer.BASE64_FORMAT) && !isBase64)
                        continue;

                    jsonObject.setTag(gatewayServer.getTag());
                    final String jsonStringBody = gson.toJson(jsonObject);
                    Log.d(getClass().getName(), "Routing: " + jsonStringBody);

                    try {
                        OneTimeWorkRequest routeMessageWorkRequest = new OneTimeWorkRequest.Builder(Router.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(TAG_NAME)
                                .addTag(TAG_WORKER_ID + messageId)
                                .addTag(TAG_ROUTING_URL + gatewayServer.getURL())
                                .setInputData(
                                        new Data.Builder()
                                                .putString(Router.SMS_JSON_OBJECT, jsonStringBody)
                                                .putString(Router.SMS_JSON_ROUTING_URL, gatewayServer.getURL())
                                                .build()
                                )
                                .build();

                        // String uniqueWorkName = address + message;
                        String uniqueWorkName = messageId + ":" + gatewayServer.getURL();
                        WorkManager workManager = WorkManager.getInstance(context);
                        workManager.enqueueUniqueWork(
                                uniqueWorkName,
                                ExistingWorkPolicy.KEEP,
                                routeMessageWorkRequest);
                    } catch (Exception e) {
                        throw e;
                    }
                }
                databaseConnector.close();
            }
        }).start();
    }
}
