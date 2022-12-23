package com.example.swob_deku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class MMSReceiverActivity extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New MMS received..");
    }
}
