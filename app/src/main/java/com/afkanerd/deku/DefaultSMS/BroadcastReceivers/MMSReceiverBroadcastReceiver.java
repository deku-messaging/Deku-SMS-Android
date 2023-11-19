package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.BuildConfig;


public class MMSReceiverBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "New MMS received..");
    }
}
