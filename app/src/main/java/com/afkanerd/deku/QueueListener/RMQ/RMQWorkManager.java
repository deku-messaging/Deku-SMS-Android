package com.afkanerd.deku.QueueListener.RMQ;

import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;

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

import com.afkanerd.deku.DefaultSMS.ConversationThreadsActivity;
import com.afkanerd.deku.DefaultSMS.R;

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
        if(!sharedPreferences.getAll().isEmpty()) {
            try {
                context.startForegroundService(intent);
                new RMQConnectionService().createForegroundNotification(0,
                        sharedPreferences.getAll().size());
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
        Intent notificationIntent = new Intent(getApplicationContext(), ConversationThreadsActivity.class);
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
