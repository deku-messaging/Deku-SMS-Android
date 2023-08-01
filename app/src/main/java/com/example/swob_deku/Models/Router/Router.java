package com.example.swob_deku.Models.Router;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.swob_deku.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Router extends Worker {
    public static final String SMS_TYPE_INCOMING = "SMS_TYPE_INCOMING";
    public static final String SMS_JSON_OBJECT = "SMS_JSON_OBJECT";
    public static final String SMS_JSON_ROUTING_URL = "SMS_JSON_ROUTING_URL";
    // https://developer.android.com/topic/libraries/architecture/workmanager/basics
    public Router(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
//            String address = getInputData().getString("address");
//            String text = getInputData().getString("text");
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
