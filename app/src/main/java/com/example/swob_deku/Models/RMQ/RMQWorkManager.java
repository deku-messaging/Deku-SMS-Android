package com.example.swob_deku.Models.RMQ;

import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.swob_deku.GatewayClientCustomizationActivity;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.MessagesThreadsActivity;
import com.example.swob_deku.Models.Web.WebWebsocketsService;
import com.example.swob_deku.R;

public class RMQWorkManager extends Worker {
    final int NOTIFICATION_ID = 12345;
    Context context;

    SharedPreferences sharedPreferences;

    public RMQWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        sharedPreferences = context.getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent intent = new Intent(getApplicationContext(), RMQConnectionService.class);
//        _startWebsocketsServices();
        if(!sharedPreferences.getAll().isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (e instanceof ForegroundServiceStartNotAllowedException) {
                            notifyUserToReconnectSMSServices();
                        }
                    }
                    return Result.failure();
                }
            } else {
                context.startService(intent);
            }
            return Result.success();
        }
        return null;
    }

    private void _startWebsocketsServices(){
        Intent intent = new Intent(getApplicationContext(), WebWebsocketsService.class);
        Log.d(getClass().getName(), "+ Starting websockets...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void notifyUserToReconnectSMSServices(){
        Intent notificationIntent = new Intent(getApplicationContext(), MessagesThreadsActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(),
                        context.getString(R.string.foreground_service_failed_channel_id))
                        .setContentTitle(context.getString(R.string.foreground_service_failed_channel_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentText(context.getString(R.string.foreground_service_failed_channel_description))
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}
