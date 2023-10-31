package com.afkanerd.deku.DefaultSMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.afkanerd.deku.DefaultSMS.Models.RoomViewModel;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;

public class CustomAppCompactActivity extends AppCompatActivity {
    BroadcastReceiver nativeStateChangedBroadcastReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!_checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        configureBroadcastListeners(null);
    }

    private boolean _checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    public void configureBroadcastListeners(RoomViewModel viewModel) {
        nativeStateChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                Log.d(getClass().getName(), "Native state changed broadcast intent");
            }
        };

        registerReceiver(nativeStateChangedBroadcastReceiver,
                new IntentFilter(SMSHandler.NATIVE_STATE_CHANGED_BROADCAST_INTENT),
                Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nativeStateChangedBroadcastReceiver != null)
            unregisterReceiver(nativeStateChangedBroadcastReceiver);
    }

    public void cancelNotifications(String threadId) {
        if (!threadId.isEmpty()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                    getApplicationContext());
            notificationManager.cancel(Integer.parseInt(threadId));
        }
    }

}
