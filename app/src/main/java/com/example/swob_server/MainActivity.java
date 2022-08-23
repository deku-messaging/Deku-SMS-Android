package com.example.swob_server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.swob_server.Models.DHKeyAgreement2;
import com.example.swob_server.Models.SMSHandler;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MainActivity extends AppCompatActivity {

    public static final int READ_SMS_PERMISSION_REQUEST_CODE = 1;
    public static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            DHKeyAgreement2.test();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        createNotificationChannel();
        handleIncomingMessage();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceStates) {
        super.onPostCreate(savedInstanceStates);

        checkIsDefaultApp();
//        if(!checkPermissionToReadSMSMessages()) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST_CODE);
//        }
//        else {
//            if(!checkPermissionToReadContacts()) {
//                ActivityCompat.requestPermissions(
//                        this,
//                        new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_PERMISSION_REQUEST_CODE);
//            }
//            else {
//                startActivity(new Intent(this, MessagesThreadsActivity.class));
//                finish();
//            }
//        }
    }

    private void checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        if (defaultPackage == null || !myPackageName.equals(defaultPackage)) {
            Toast.makeText(this, "I'm not your default app.", Toast.LENGTH_LONG).show();
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
        else {
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


    private void createNotificationChannel() {
        // TODO: Read more: https://developer.android.com/training/notify-user/channels
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = getString(R.string.channel_name);

            String description = getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(getString(R.string.CHANNEL_ID), name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(R.color.white);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public boolean checkPermissionToReadSMSMessages() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_SMS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    if(checkPermissionToReadContacts())
                        startActivity(new Intent(this, MessagesThreadsActivity.class));
                    else {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_PERMISSION_REQUEST_CODE);
                    }
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case READ_CONTACTS_PERMISSION_REQUEST_CODE:
                startActivity(new Intent(this, MessagesThreadsActivity.class));
                finish();
                break;

        }
    }

    private void handleIncomingMessage() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    for (SmsMessage currentSMS: Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        // TODO: Fetch address name from contact list if present
                        String address = currentSMS.getDisplayOriginatingAddress();

                        String message = currentSMS.getDisplayMessageBody();
                        SMSHandler.registerIncomingMessage(context, currentSMS);
                        sendNotification(message, address);
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    private void sendNotification(String text, String address) {
        Intent receivedSmsIntent = new Intent(this, SendSMSActivity.class);
        receivedSmsIntent.putExtra(SendSMSActivity.ADDRESS, address);
        receivedSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingReceivedSmsIntent = PendingIntent.getActivity(
                this, 0, receivedSmsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), getString(R.string.CHANNEL_ID))
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New SMS from " + address)
                .setContentText(text)
                .setContentIntent(pendingReceivedSmsIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(8888, builder.build());
    }

}