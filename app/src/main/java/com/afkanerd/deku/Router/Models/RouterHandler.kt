package com.afkanerd.deku.Router.Models;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.R;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RouterHandler {
    protected static ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static void routeFTPMessages(final String body, GatewayServer gatewayServer) throws Exception {
        Log.d(RouterHandler.class.getName(), "Request to route - FTP: " + body);

        // TODO: move TLS into a parameter configuration
        FTPSClient ftpsClient = new FTPSClient("TLS");

        ftpsClient.connect(gatewayServer.ftp.ftp_host);
        ftpsClient.login(gatewayServer.ftp.ftp_username, gatewayServer.ftp.ftp_password);
        ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpsClient.enterLocalPassiveMode();

        if(ftpsClient.getReplyCode() >= 200 && ftpsClient.getReplyCode() < 300) {
            // TODO: move this to a parameter configuration
            ftpsClient.execPBSZ(0); // Set protection buffer size (optional)
            ftpsClient.execPROT("P"); // Set protection mode to private (optional)

            // TODO: move this to a parameter configuration
//            if(!ftpsClient.changeWorkingDirectory(gatewayServer.ftp.ftp_working_directory)) {
//                ftpsClient.makeDirectory(gatewayServer.ftp.ftp_remote_path);
//                ftpsClient.changeWorkingDirectory(gatewayServer.ftp.ftp_working_directory);
//            }

            if(gatewayServer.ftp.ftp_remote_path == null)
                gatewayServer.ftp.ftp_remote_path = System.currentTimeMillis() + ".json";
            boolean stored = ftpsClient.storeFile(gatewayServer.ftp.ftp_remote_path,
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            ftpsClient.disconnect();
            if(!stored) {
                throw new Exception("Failed to write file to FTP server");
            }
        }
    }

    public static void routeSmtpMessages(final String body, GatewayServer gatewayServer)
            throws MessagingException {
        Log.d(RouterHandler.class.getName(), "Request to route - SMTP: " + body);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", gatewayServer.smtp.smtp_host);
        properties.put("mail.smtp.port", gatewayServer.smtp.smtp_port);
        properties.put("mail.smtp.starttls.enable", "true");

        if(BuildConfig.DEBUG)
            properties.put("mail.debug", "true");

        /**
         * TODO
         *  if (authenticationRequired) {
         *             Authenticator auth = new SMTPAuthenticator();
         *             props.put("mail.smtp.auth", "true");
         *             session = Session.getDefaultInstance(props, auth);
         *         } else {
         *             session = Session.getDefaultInstance(props, null);
         *         }
         */

        InternetAddress[] internetAddresses = InternetAddress.parse(gatewayServer.smtp.smtp_recipient);
        Session session = Session.getInstance(properties, null);
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gatewayServer.smtp.smtp_from));
        message.setRecipients(Message.RecipientType.TO, internetAddresses);
        message.setSubject(gatewayServer.smtp.smtp_subject);
        message.setSentDate(new Date());
        message.setText(body);

        Transport.send(message, gatewayServer.smtp.smtp_username, gatewayServer.smtp.smtp_password);
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
        return tag.split(":")[1];
    }

    public static String getTagForGatewayServers(long gatewayServerId) {
        return TAG_GATEWAY_SERVER_ID + gatewayServerId;
    }

    public static String getGatewayServerIdFromTag(String tag) {
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
