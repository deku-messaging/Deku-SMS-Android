package com.example.swob_deku.Models;

import static android.content.pm.PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class SIMHandler {

    public static List<SubscriptionInfo> getSimCardInformation(Context context) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        int simCount = getActiveSimcardCount(context);

        return subscriptionManager.getActiveSubscriptionInfoList();
//        for (int simSlot = 0; simSlot < simcards.size(); simSlot++) {
//            String simOperatorName = subscriptionManager.get
//            String simCountryIso = telephonyManager.getSimCountryIso(simSlot);
//            String simState = getSimStateString(telephonyManager.getSimState(simSlot));
//
//            String TAG = SIMHandler.class.getName();
//
//            Log.d(TAG, "Sim Slot: " + simSlot);
//            Log.d(TAG, "Serial Number: " + simSerialNumber);
//            Log.d(TAG, "Operator Name: " + simOperatorName);
//            Log.d(TAG, "Country ISO: " + simCountryIso);
//            Log.d(TAG, "Sim State: " + simState);
//        }
    }

    public static int getActiveSimcardCount(Context context) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(SIMHandler.class.getName(), "Sim card not granted!");
        }
        else
            Log.d(SIMHandler.class.getName(), "Sim card granted!");
        return subscriptionManager.getActiveSubscriptionInfoCount();
    }

    private static String getSimStateString(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "Absent";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "Network locked";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK required";
            case TelephonyManager.SIM_STATE_READY:
                return "Ready";
            case TelephonyManager.SIM_STATE_UNKNOWN:
            default:
                return "Unknown";
        }
    }
    public static int getDefaultSimSubscription(Context context) {
//        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        int defaultSmsSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
        SubscriptionInfo subscriptionInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(defaultSmsSubscriptionId);

        return subscriptionInfo.getSubscriptionId();
    }
}
