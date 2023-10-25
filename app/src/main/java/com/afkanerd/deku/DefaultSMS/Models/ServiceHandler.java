package com.afkanerd.deku.DefaultSMS.Models;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ServiceHandler {

    public static List<ActivityManager.RunningServiceInfo> getRunningService(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getRunningServices(Integer.MAX_VALUE);
    }
}
