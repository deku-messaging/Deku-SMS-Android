package com.afkanerd.deku.Router.Models;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.Router.FTP;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;
import com.afkanerd.deku.Router.SMTP;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RouterWorkManager extends Worker {
    public static String GATEWAY_SERVER_ID = "GATEWAY_SERVER_ID";
    public static String CONVERSATION_ID = "CONVERSATION_ID";

    // https://developer.android.com/topic/libraries/architecture/workmanager/basics
    public RouterWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long gatewayServerId = getInputData().getLong(GATEWAY_SERVER_ID, -1);
        String conversationId = getInputData().getString(CONVERSATION_ID);

        Datastore datastore = Datastore.getDatastore(getApplicationContext());
        GatewayServer gatewayServer = datastore.gatewayServerDAO()
                .get(String.valueOf(gatewayServerId));
        Conversation conversation = datastore.conversationDao().getMessage(conversationId);

        RouterItem routerItem = new RouterItem(conversation);
        routerItem.tag = gatewayServer.getTag();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting().serializeNulls();
        Gson gson = gsonBuilder.create();
        final String jsonStringBody = gson.toJson(routerItem);

        if(gatewayServer.getProtocol() != null &&
                gatewayServer.getProtocol().equals(SMTP.PROTOCOL)) {
            if(jsonStringBody != null) {
                try {
                    RouterHandler.routeSmtpMessages(jsonStringBody, gatewayServer);
                } catch(Exception e) {
                    Log.e(getClass().getName(), "Exception: ", e);
                    return Result.failure();
                }
            } else {
                return Result.failure();
            }
        } else if(gatewayServer.getProtocol() != null &&
                gatewayServer.getProtocol().equals(FTP.PROTOCOL)) {
            if(jsonStringBody != null) {
                try {
                    RouterHandler.routeFTPMessages(jsonStringBody, gatewayServer);
                } catch(Exception e) {
                    Log.e(getClass().getName(), "Exception: ", e);
                    return Result.failure();
                }
            } else {
                return Result.failure();
            }
        }
        else {
            if(jsonStringBody != null) {
                try {
                    RouterHandler.routeJsonMessages(getApplicationContext(), jsonStringBody,
                            gatewayServer.getURL());
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    Log.e(getClass().getName(), "Exception: ", e);
                    if (e.getCause() instanceof ServerError) {
                        ServerError error = (ServerError) e.getCause();
                        if (error.networkResponse.statusCode >= 400)
                            return Result.failure();
                    }
                    else if(e.getCause() instanceof ParseError) {
                        return Result.success();
                    }
                    return Result.retry();
                } catch (Exception e ) {
                    Log.e(getClass().getName(), "Exception: ", e);
                    return Result.failure();
                }
            } else {
                return Result.failure();
            }
        }
        return Result.success();
    }
}
