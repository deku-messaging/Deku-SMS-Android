package com.afkanerd.deku.Router.Router;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.compose.runtime.State;
import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.R;
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
import java.util.Set;
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
    protected static ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static void routeSmtpMessages(final String body, GatewayServer gatewayServer)
            throws MessagingException {
        Log.d(RouterHandler.class.getName(), "Request to route - SMTP: " + body);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", gatewayServer.smtp.host);
        properties.put("mail.smtp.port", gatewayServer.smtp.port);

        if(BuildConfig.DEBUG)
            properties.put("mail.debug", "true");

        InternetAddress[] internetAddresses = InternetAddress.parse(gatewayServer.smtp.recipient);
        Session session = Session.getInstance(properties, null);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gatewayServer.smtp.from));
        message.setRecipients(Message.RecipientType.TO, internetAddresses);
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

    public static void removeWorkForGatewayServers(Context context, long gatewayServerId) {
        String tag = getTagForGatewayServers(gatewayServerId);
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(tag);
    }

    public static final String TAG_NAME_GATEWAY_SERVER = "TAG_NAME_GATEWAY_SERVER";
    public static final String TAG_GATEWAY_SERVER_MESSAGE_ID = "TAG_GATEWAY_SERVER_MESSAGE_ID:";
    public static final String TAG_GATEWAY_SERVER_ID = "TAG_GATEWAY_SERVER_ID:";
    public static String getTagForMessages(String messageId) {
        return TAG_GATEWAY_SERVER_MESSAGE_ID + messageId;
    }

    public static String getMessageIdFromTag(String tag) {
        Log.d(RouterHandler.class.getName(), "Getting message ID from tag: " + tag);
        return tag.split(":")[1];
    }

    public static String getTagForGatewayServers(long gatewayServerId) {
        return TAG_GATEWAY_SERVER_ID + gatewayServerId;
    }

    public static String getGatewayServerIdFromTag(String tag) {
        Log.d(RouterHandler.class.getName(), "Getting server ID from tag: " + tag);
        return tag.split(":")[1];
    }

    public static String reverseState(Context context, WorkInfo.State state) {
        String stateValue;
        switch(state) {
            case SUCCEEDED:
                stateValue = context.getString(R.string.gateway_server_routing_state_success);
                break;
            case ENQUEUED:
                stateValue = context.getString(R.string.gateway_server_routing_state_enqueued);
                break;
            case FAILED:
                stateValue = context.getString(R.string.gateway_server_routing_state_failed);
                break;
            case RUNNING:
                stateValue = context.getString(R.string.gateway_server_routing_state_running);
                break;
            case CANCELLED:
                stateValue = context.getString(R.string.gateway_server_routing_state_cancelled);
                break;
            default:
                stateValue = "";
        }
        return stateValue;
    }

    public static LiveData<List<WorkInfo>> getMessageIdsFromWorkManagers(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        return workManager.getWorkInfosByTagLiveData(TAG_NAME_GATEWAY_SERVER);

    }

    public static Pair<String, String> workInfoParser(WorkInfo workInfo) {
        String messageId = "", gatewayServerId = "";
        for(String tag : workInfo.getTags()) {
            Log.d(RouterHandler.class.getName(), "Tags: " + tag);
            if (tag.contains(TAG_GATEWAY_SERVER_ID)) {
                gatewayServerId = getGatewayServerIdFromTag(tag);
            }
            if (tag.contains(TAG_GATEWAY_SERVER_MESSAGE_ID)) {
                messageId = getMessageIdFromTag(tag);
            }
        }
        return new Pair<>(messageId, gatewayServerId);
    }
}
