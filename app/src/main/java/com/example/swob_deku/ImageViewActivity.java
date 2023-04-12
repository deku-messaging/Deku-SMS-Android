package com.example.swob_deku;

import static com.example.swob_deku.SMSSendActivity.SMS_DELIVERED_INTENT;
import static com.example.swob_deku.SMSSendActivity.SMS_SENT_INTENT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.io.IOException;
import java.util.ArrayList;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;

    TextView imageDescription;

    Bitmap compressedBitmap;
    byte[] compressedBytes;

    String address = "";
    String threadId = "";

    ImageHandler imageHandler;

    final int MIN_RESOLUTION = 256;

    public static final String IMAGE_INTENT_EXTRA = "image_sms_id";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.image_view_toolbar);
//        myToolbar.inflateMenu(R.menu.default_menu);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        imageView = findViewById(R.id.compressed_image_holder);
        imageDescription = findViewById(R.id.image_details_size);

        if(getIntent().hasExtra(IMAGE_INTENT_EXTRA)) {
            String smsId = getIntent().getStringExtra(IMAGE_INTENT_EXTRA);

            // TODO: Get all messages which have the Ref ID
            // TODO: get until the len of messages have been acquired, then fit them together
            // TODO: until the len has been acquired.

            Cursor cursor = SMSHandler.fetchSMSInboxById(getApplicationContext(), smsId);
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);

                byte[] body = Base64.decode(sms.getBody()
                        .replace(ImageHandler.IMAGE_HEADER, ""), Base64.DEFAULT);
                try {
                    buildImage(body);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            cursor.close();
        }
        else {
            address = getIntent().getStringExtra(SMSSendActivity.ADDRESS);
            threadId = getIntent().getStringExtra(SMSSendActivity.THREAD_ID);
            imageUri = Uri.parse(getIntent().getStringExtra(SMSSendActivity.IMAGE_URI));

            try {
                imageHandler = new ImageHandler(getApplicationContext(), imageUri);

                ((TextView)findViewById(R.id.image_details_original_resolution))
                        .setText("Original resolution: "
                                + imageHandler.bitmap.getWidth()
                                + " x "
                                + imageHandler.bitmap.getHeight());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                int maxResolution = buildImage(MIN_RESOLUTION);
                changeResolution(maxResolution);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void changeResolution(int maxResolution) {
        final double resDifference = maxResolution - MIN_RESOLUTION;
        final double changeConstant = resDifference / 100;

        SeekBar seekBar = findViewById(R.id.image_view_change_resolution_seeker);

        TextView seekBarProgress = findViewById(R.id.image_details_seeker_progress);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            double newMaxResolution = MIN_RESOLUTION;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO: change the resolution text
                newMaxResolution = progress == 0 ? MIN_RESOLUTION : MIN_RESOLUTION + (changeConstant * progress);
                Log.d(getLocalClassName(), "New resolution = " + newMaxResolution);
                seekBarProgress.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO: compress the image
                try {
                    buildImage(newMaxResolution);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void buildImage(byte[] data ) throws IOException {
        compressedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        imageView.setImageBitmap(compressedBitmap);
    }

    private int buildImage(double newResolution) throws IOException {
        // TODO: messages >40 trigger large message warning...
        int maxresolution = imageHandler.getMaxResolution();

//        final int SCALE_DOWN_RATIO = 3;
        final int COMPRESSION_RATIO = 15;

//        int resizeScale = imageHandler.bitmap.getWidth() / SCALE_DOWN_RATIO;

        Bitmap imageBitmap = imageHandler.resizeImage(newResolution);

        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                getSystemService(SmsManager.class) : SmsManager.getDefault();

        compressedBytes = imageHandler.compressImage(COMPRESSION_RATIO, imageBitmap);

        ArrayList<String> dividedArray = smsManager.divideMessage(
                Base64.encodeToString(compressedBytes, Base64.DEFAULT));

//        byte[] riffHeader = SMSHandler.copyBytes(compressedBytes, 0, 12);
//        byte[] vp8Header = SMSHandler.copyBytes(compressedBytes, 12, 4);

        TextView imageResolution = findViewById(R.id.image_details_resolution);
        imageResolution.setText("New resolution " + imageBitmap.getWidth() + " x " + imageBitmap.getHeight());

        TextView imageSize = findViewById(R.id.image_details_size);
        imageSize.setText("Size " + (compressedBytes.length / 1024) + " KB");

        TextView imageQuality = findViewById(R.id.image_details_quality);
        imageQuality.setText("Quality " + COMPRESSION_RATIO + "%");

        TextView imageSMSCount = findViewById(R.id.image_details_sms_count);
        imageSMSCount.setText(dividedArray.size() + " Messages");

        compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
        imageView.setImageBitmap(compressedBitmap);

        return maxresolution;
    }

    public void handleBroadcast() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)

        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                long id = intent.getLongExtra(SMSSendActivity.ID, -1);
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Broadcast received for sent: " + id);
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        try {
                            SMSHandler.registerSentMessage(getApplicationContext(), id);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    default:
                        try {
                            SMSHandler.registerFailedMessage(context, id, getResultCode());
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        if(BuildConfig.DEBUG) {
                            Log.d(getLocalClassName(), "Failed to send: " + getResultCode());
                            Log.d(getLocalClassName(), "Failed to send: " + getResultData());
                            Log.d(getLocalClassName(), "Failed to send: " + intent.getData());
                        }
                }

                unregisterReceiver(this);
            }
        };

        BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(SMSSendActivity.ID, -1);

                if (getResultCode() == Activity.RESULT_OK) {
                    SMSHandler.registerDeliveredMessage(context, id);
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(getLocalClassName(), "Failed to deliver: " + getResultCode());
                }

                unregisterReceiver(this);
            }
        };

        registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_INTENT));
        registerReceiver(sentBroadcastReceiver, new IntentFilter(SMS_SENT_INTENT));

    }

    public void sendImage(View view) throws InterruptedException {
        Intent intent = new Intent(this, SMSSendActivity.class);
        intent.putExtra(SMSSendActivity.ADDRESS, address);

        if(!threadId.isEmpty())
            intent.putExtra(SMSSendActivity.THREAD_ID, threadId);

        startActivity(intent);

        handleBroadcast();

        long messageId = Helpers.generateRandomNumber();

        PendingIntent[] pendingIntents = SMSSendActivity.getPendingIntents(getApplicationContext(),
                messageId);

        int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
        SMSHandler.sendTextSMS(getApplicationContext(), address,
                ImageHandler.IMAGE_HEADER + Base64.encodeToString(compressedBytes, Base64.DEFAULT),
                pendingIntents[0],
                pendingIntents[1],
                messageId, subscriptionId);

        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SMSSendActivity.class);
        intent.putExtra(SMSSendActivity.ADDRESS, address);

        if(!threadId.isEmpty())
            intent.putExtra(SMSSendActivity.THREAD_ID, threadId);

        startActivity(intent);
        finish();
    }
}