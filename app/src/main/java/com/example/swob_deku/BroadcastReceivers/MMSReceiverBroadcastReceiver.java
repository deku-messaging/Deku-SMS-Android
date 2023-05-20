package com.example.swob_deku.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.swob_deku.BuildConfig;


public class MMSReceiverBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New MMS received..");
    }
}
