package com.afkanerd.deku.QueueListener.RMQ;


import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;

public class RMQWorkManager extends Worker {
    final int NOTIFICATION_ID = 12345;

    SharedPreferences sharedPreferences;

    public RMQWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent intent = new Intent(getApplicationContext(), RMQConnectionService.class);
        sharedPreferences = getApplicationContext()
                .getSharedPreferences(GatewayClientListingActivity.Companion
                        .getGATEWAY_CLIENT_LISTENERS(), Context.MODE_PRIVATE);
        if(!sharedPreferences.getAll().isEmpty()) {
            try {
                getApplicationContext().startForegroundService(intent);
                RMQConnectionService rmqConnectionService =
                        new RMQConnectionService(getApplicationContext());
//                rmqConnectionService.createForegroundNotification(0,
//                        sharedPreferences.getAll().size());
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof ForegroundServiceStartNotAllowedException) {
                    notifyUserToReconnectSMSServices();
                }
                return Result.failure();
            }
        }
        return Result.success();
    }

    private void notifyUserToReconnectSMSServices(){
        Intent notificationIntent = new Intent(getApplicationContext(), ThreadedConversationsActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        Notification notification =
                new NotificationCompat.Builder(getApplicationContext(),
                        getApplicationContext().getString(R.string.foreground_service_failed_channel_id))
                        .setContentTitle(getApplicationContext()
                                .getString(R.string.foreground_service_failed_channel_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentText(getApplicationContext()
                                .getString(R.string.foreground_service_failed_channel_description))
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}
