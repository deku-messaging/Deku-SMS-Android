package com.afkanerd.deku.DefaultSMS.Models;

import static android.content.Context.TELEPHONY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.List;

public class SIMHandler {

    public static List<SubscriptionInfo> getSimCardInformation(Context context) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        return subscriptionManager.getActiveSubscriptionInfoList();
    }

    public static boolean isDualSim(Context context) {
        TelephonyManager manager = (TelephonyManager)context.getSystemService(TELEPHONY_SERVICE);
        return manager.getPhoneCount() > 1;
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
        int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
        if(subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            return getSimCardInformation(context).get(0).getSubscriptionId();
        return subId;
    }

    public static String getSubscriptionName(Context context, int subscriptionId) {
        List<SubscriptionInfo> subscriptionInfos = getSimCardInformation(context);

        for(SubscriptionInfo subscriptionInfo : subscriptionInfos)
            if(subscriptionInfo.getSubscriptionId() == subscriptionId) {
                if(subscriptionInfo.getCarrierName() != null)
                    return subscriptionInfo.getDisplayName().toString();
            }
        return "";
    }
}
