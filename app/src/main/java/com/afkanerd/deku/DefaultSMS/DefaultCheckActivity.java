package com.afkanerd.deku.DefaultSMS;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DefaultCheckActivity extends AppCompatActivity {

    public static final int READ_SMS_PERMISSION_REQUEST_CODE = 1;
    public static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_check);

        MaterialButton materialButton = findViewById(R.id.default_check_make_default_btn);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDefault(v);
            }
        });
    }

//    public void quicktest() {
//        Intent intent = new Intent(Intent.ACTION_SENDTO);
//        intent.setData(Uri.parse("smsto:"));
//
//        // Set the recipient phone number
//        intent.putExtra("address", "123456789; 1234567890; 12345678901");
//        // here i can send message to emulator 5556,5558,5560
//        // you can change in real device
//        intent.putExtra("sms_body", "Hello friends!");
//        startActivity(intent);
//    }

    public void clickPrivacyPolicy(View view) {
        String url = getString(R.string.privacy_policy_url);
        Intent shareIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(shareIntent);
    }

    public void makeDefault(View view) {
        Log.d(getLocalClassName(), "Got into make default function..");
        final String myPackageName = getApplicationContext().getPackageName();
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
            startUserActivities();
        }
    }

    private void startUserActivities() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    createNotificationChannel();
                }
                startServices();
                finish();
            }
        }).start();

        Intent intent = new Intent(this, ThreadedConversationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (reqCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sharedPreferences.edit()
                        .putBoolean(getString(R.string.configs_load_natives), true)
                        .apply();
                startUserActivities();
            }
        }
    }

    ArrayList<String> notificationsChannelIds = new ArrayList<>();
    ArrayList<String> notificationsChannelNames = new ArrayList<>();

    private List<String> clearOutOldNotificationChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        List<String> notificationChannelList = new ArrayList<>();

        for(NotificationChannel notificationChannel : notificationManager.getNotificationChannels()) {
            if(!notificationsChannelIds.contains(notificationChannel.getId()))
                notificationManager.deleteNotificationChannel(notificationChannel.getId());
            else
                notificationChannelList.add(notificationChannel.getId());
        }

        return notificationChannelList;
    }

    private void createNotificationChannelIncomingMessage() {
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(
                notificationsChannelIds.get(0), notificationsChannelNames.get(0), importance);
        channel.setDescription(getString(R.string.incoming_messages_channel_description));
        channel.enableLights(true);
        channel.setLightColor(R.color.logo_primary);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannelRunningGatewayListeners() {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    notificationsChannelIds.get(1), notificationsChannelNames.get(1), importance);
            channel.setDescription(getString(R.string.running_gateway_clients_channel_description));
            channel.setLightColor(R.color.logo_primary);
            channel.setLockscreenVisibility(Notification.DEFAULT_ALL);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannelReconnectGatewayListeners() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(
                notificationsChannelIds.get(2), notificationsChannelNames.get(2), importance);
        channel.setDescription(getString(R.string.running_gateway_clients_channel_description));
        channel.setLightColor(R.color.logo_primary);
        channel.setLockscreenVisibility(Notification.DEFAULT_ALL);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannel() {
        notificationsChannelIds.add(getString(R.string.incoming_messages_channel_id));
        notificationsChannelNames.add(getString(R.string.incoming_messages_channel_name));

        notificationsChannelIds.add(getString(R.string.running_gateway_clients_channel_id));
        notificationsChannelNames.add(getString(R.string.running_gateway_clients_channel_name));

        notificationsChannelIds.add(getString(R.string.foreground_service_failed_channel_id));
        notificationsChannelNames.add(getString(R.string.foreground_service_failed_channel_name));

        createNotificationChannelIncomingMessage();

        createNotificationChannelRunningGatewayListeners();

        createNotificationChannelReconnectGatewayListeners();
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIsDefaultApp();
    }

    private void startServices() {
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        try {
            gatewayClientHandler.startServices();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            gatewayClientHandler.close();
        }

    }

}