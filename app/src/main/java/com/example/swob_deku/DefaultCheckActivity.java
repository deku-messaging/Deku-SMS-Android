package com.example.swob_deku;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultCheckActivity extends AppCompatActivity {

    public static final int READ_SMS_PERMISSION_REQUEST_CODE = 1;
    public static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 2;

    CharSequence incomingNotificationsName;

    String incomingNotificationsDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_check);
        incomingNotificationsName = getString(R.string.channel_name);
        incomingNotificationsDescription = getString(R.string.channel_description);
    }

    public void clickPrivacyPolicy(View view) {
        String url = "https://smswithoutborders.com/privacy-policy";
        Intent shareIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(shareIntent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceStates) {
        super.onPostCreate(savedInstanceStates);

    }

    public void makeDefault(View view) {
        final String myPackageName = getPackageName();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            Intent roleManagerIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
            roleManagerIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
            startActivityForResult(roleManagerIntent, 0);
        }
        else {
            Intent intent =
                    new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
            startActivity(intent);
        }
    }

    private void checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        if (myPackageName.equals(defaultPackage)) {
            createNotificationChannel();
            startActivity(new Intent(this, MessagesThreadsActivity.class));
            finish();
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case 0:
                if(resultCode == Activity.RESULT_OK) {
                    startActivity(new Intent(this, MessagesThreadsActivity.class));
                }
            default:
                finish();
        }
    }

    private List<String> clearOutOldNotificationChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        List<String> notificationChannelList = new ArrayList<>();
        for(NotificationChannel notificationChannel : notificationManager.getNotificationChannels()) {
            if(!notificationChannel.getId().equals(incomingNotificationsName))
                notificationManager.deleteNotificationChannel(notificationChannel.getId());
            notificationChannelList.add(notificationChannel.getId());
        }

        return notificationChannelList;
    }


    private void createNotificationChannel() {
        // TODO: Read more: https://developer.android.com/training/notify-user/channels
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!clearOutOldNotificationChannels().contains(incomingNotificationsName)) {
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel channel = new NotificationChannel(getString(R.string.CHANNEL_ID), incomingNotificationsName, importance);
                channel.setDescription(incomingNotificationsDescription);
                channel.enableLights(true);
                channel.setLightColor(R.color.logo_primary);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (checkPermissionToReadContacts())
                    startActivity(new Intent(this, MessagesThreadsActivity.class));
                else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_PERMISSION_REQUEST_CODE);
                }
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIsDefaultApp();
    }
}