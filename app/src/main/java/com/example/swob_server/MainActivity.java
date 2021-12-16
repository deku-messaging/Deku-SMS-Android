package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel("swob_server", "notifies swob", "123456");
        get_messages_old();
    }

    public void get_messages_old() {
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                int bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY);
                int numberIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
                Log.i(this.getLocalClassName(), "BODY " + String.valueOf(cursor.getString(bodyIndex)));
                Log.i(this.getLocalClassName(), "NUMBER " + String.valueOf(cursor.getString(numberIndex)));
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
            Log.i(this.getLocalClassName(), "No messages to show");
        }
    }


    private void createNotificationChannel(String channel_name, String channel_description, String channel_id) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(this.getLocalClassName(), "Creating notification Channel");
//            CharSequence name = getString(R.string.channel_name);
//            String description = getString(R.string.channel_description);
            CharSequence name = channel_name;
            String description = channel_description;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}