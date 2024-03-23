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
import com.afkanerd.deku.Router.GatewayServers.GatewayServerHandler;
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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RouterHandler {
    public static int MESSAGE_ID = 0;
    public static int WORK_NAME = 1;
    public static int ROUTING_URL = 2;
    public static int ROUTING_ID = 3;

    protected static ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static void routeSmtpMessages(final String body, GatewayServer gatewayServer)
            throws MessagingException {
        Log.d(RouterHandler.class.getName(), "Request to route - SMTP: " + body);
        Properties properties = new Properties();
        properties.put("mail.smtp.host", gatewayServer.smtp.host);
        properties.put("mail.smtp.port", gatewayServer.smtp.port);
//        properties.put("mail.debug", "true");

        Session session = Session.getInstance(properties, null);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gatewayServer.smtp.from));
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(gatewayServer.smtp.recipient));
        message.setSubject(gatewayServer.smtp.subject);
        message.setSentDate(new Date());
        message.setText(body);

        Transport.send(message, gatewayServer.smtp.username, gatewayServer.smtp.password);
    }

    public static void routeJsonMessages(Context context, String jsonStringBody, String gatewayServerUrl)
            throws ExecutionException, InterruptedException, TimeoutException, JSONException {
        Log.d(RouterHandler.class.getName(), "Request to route - HTTP: " + jsonStringBody);
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

    public static final String TAG_WORKER_ID = "TAG_WORKER_ID";
    public static String getTagForMessages(String messageId) {
        return TAG_WORKER_ID + messageId;
    }

    public static String getTagForGatewayServers(String gatewayClientUrl) {
        return TAG_ROUTING_URL + gatewayClientUrl;
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
