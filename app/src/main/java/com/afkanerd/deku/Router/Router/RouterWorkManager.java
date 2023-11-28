package com.afkanerd.deku.Router.Router;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.ParseError;
import com.android.volley.ServerError;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RouterWorkManager extends Worker {
    public static final String SMS_JSON_OBJECT = "SMS_JSON_OBJECT";
    public static final String SMS_JSON_ROUTING_URL = "SMS_JSON_ROUTING_URL";
    // https://developer.android.com/topic/libraries/architecture/workmanager/basics
    public RouterWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String gatewayServerUrl = getInputData().getString(SMS_JSON_ROUTING_URL);
            String jsonBody = getInputData().getString(SMS_JSON_OBJECT);

            if(jsonBody != null)
                RouterHandler.routeJsonMessages(getApplicationContext(), jsonBody, gatewayServerUrl);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause instanceof ServerError) {
                ServerError error = (ServerError) cause;
                int statusCode = error.networkResponse.statusCode;
                if (statusCode >= 400)
                    return Result.failure();
            }
            else if(cause instanceof ParseError) {
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e ) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }
}
