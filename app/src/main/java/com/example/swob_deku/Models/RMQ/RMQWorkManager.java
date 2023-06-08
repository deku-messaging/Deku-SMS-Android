package com.example.swob_deku.Models.RMQ;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.swob_deku.GatewayClientCustomizationActivity;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.R;

public class RMQWorkManager extends Worker {
    Context context;

    public RMQWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent intent = new Intent(getApplicationContext(), RMQConnectionService.class);
        context.startService(intent);
        return Result.success();
    }

}
