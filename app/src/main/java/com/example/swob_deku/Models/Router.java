package com.example.swob_deku.Models;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Router extends Worker {
    // https://developer.android.com/topic/libraries/architecture/workmanager/basics
    public Router(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String address = getInputData().getString("address");
            String text = getInputData().getString("text");
            routeMessagesToGatewayServers(address, text);
        } catch(ExecutionException e ) {
            e.printStackTrace();
        } catch (Exception e ) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }

    private void routeMessagesToGatewayServers(String address, String text) throws JSONException, VolleyError, ExecutionException {
        // TODO: Pause to resend if no internet connection
        // TODO: Pause till routing can happen, but should probably use a broker for this
        Context context = getApplicationContext();
        // Toast.makeText(context, "Routing messages using workers!", Toast.LENGTH_SHORT).show();
        Log.d("", "Routing: " + address + " - " + text);

        // TODO: make this come from a config file
        String gatewayServerUrl = "https://developers.smswithoutborders.com:15000/sms/platform/gateway-client";
        try{
            JSONObject jsonBody = new JSONObject( "{\"text\": \"" + text + "\", \"MSISDN\": \"" + address + "\"}");
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
            JSONObject response = future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e){
           throw e;
        } catch(InterruptedException  | TimeoutException e ) {
            e.printStackTrace();
            throw new VolleyError(e);
        } catch(JSONException e ) {
            throw e;
        } catch(Exception e ) {
            throw e;
        }
    }
}
