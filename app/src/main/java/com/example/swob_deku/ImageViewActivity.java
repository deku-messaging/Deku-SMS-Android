package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.swob_deku.Models.Compression;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;

    TextView imageDescription;

    byte[] compressedBytes;

    String address = "";
    String threadId = "";

    ImageHandler imageHandler;

    final int MAX_RESOLUTION = 400;
    final int MIN_RESOLUTION = MAX_RESOLUTION / 2;
    int COMPRESSION_RATIO = 5;

    public double changedResolution;

    public static final String IMAGE_INTENT_EXTRA = "image_sms_id";

    public static final String SMS_IMAGE_PENDING_LOCATION = "SMS_IMAGE_PENDING_LOCATION";

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

        address = getIntent().getStringExtra(SMSSendActivity.ADDRESS);
        threadId = getIntent().getStringExtra(SMSSendActivity.THREAD_ID);

        String contactName = Contacts.retrieveContactName(getApplicationContext(), address);
        contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                address: contactName;

        ab.setTitle(contactName);
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
//                changedResolution = getMaxResolution();
                changedResolution = MAX_RESOLUTION;
                buildImage();
                changeResolution(getMaxResolution());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home ) {
            Intent intent = new Intent(this, SMSSendActivity.class);
            intent.putExtra(SMSSendActivity.ADDRESS, address);

            if(!threadId.isEmpty())
                intent.putExtra(SMSSendActivity.THREAD_ID, threadId);

            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void changeResolution(final int maxResolution) {
        final double resDifference = maxResolution - MAX_RESOLUTION;
        final double changeConstant = resDifference / 100;

        SeekBar seekBar = findViewById(R.id.image_view_change_resolution_seeker);

        TextView seekBarProgress = findViewById(R.id.image_details_seeker_progress);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            final int resChangeRatio = Math.round(MIN_RESOLUTION / seekBar.getMax());
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO: change the resolution text
                double calculatedResolution = progress == 0 ? MAX_RESOLUTION :
                        MAX_RESOLUTION - (resChangeRatio * progress);
//
//                if(calculatedResolution > MIN_RESOLUTION) {
//                    changedResolution = calculatedResolution;
//                    COMPRESSION_RATIO = 0;
//                } else {
//                    changedResolution = MIN_RESOLUTION;
//                    COMPRESSION_RATIO = seekBar.getMax() - progress;
//                }
                changedResolution = calculatedResolution;
                COMPRESSION_RATIO = progress;
                seekBarProgress.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO: put loader
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO: compress the image
                try {
                    buildImage();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void buildImage(byte[] data ) throws IOException {
        TextView imageResolutionOriginal = findViewById(R.id.image_details_original_resolution);
        imageResolutionOriginal.setVisibility(View.GONE);

        TextView imageResolution = findViewById(R.id.image_details_resolution);
        imageResolution.setVisibility(View.GONE);

        TextView imageSize = findViewById(R.id.image_details_size);
        imageSize.setVisibility(View.GONE);

        TextView imageQuality = findViewById(R.id.image_details_quality);
        imageQuality.setVisibility(View.GONE);

        TextView imageSMSCount = findViewById(R.id.image_details_sms_count);
        imageSMSCount.setVisibility(View.GONE);

        TextView seekBarText = findViewById(R.id.image_details_seeker_progress);
        seekBarText.setVisibility(View.GONE);

        SeekBar seekBar = findViewById(R.id.image_view_change_resolution_seeker);
        seekBar.setVisibility(View.GONE);

        Button button = findViewById(R.id.image_send_btn);
        button.setVisibility(View.GONE);

        Bitmap compressedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        imageView.setImageBitmap(compressedBitmap);
//        compressedBitmap.recycle();
    }

    private int getMaxResolution() {
        return imageHandler.getMaxResolution();
    }

    private byte[] compress(byte[] input) {

//        byte[] compressGzip = Compression.compressLZ4(compressDeflate);
//        Log.d(getLocalClassName(), "Gzip compression: " + compressGzip.length);


//        for(int i=0;i<decompressGZIP.length; ++i) {
//            if(decompressGZIP[i] != c[i]) {
//                Log.d(getLocalClassName(), "Different things came back!");
//                break;
//            }
//        }
//        return compressGzip;
        return Compression.compressDeflate(input);
//        return input;
    }

    private byte[] decompress(byte[] input) throws DataFormatException {
//        byte[] decompressGZIP = Compression.decompressGZIP(input);
//        Log.d(getLocalClassName(), "Gzip decompressed: " + decompressGZIP.length);
//
//        return Compression.decompressDeflate(decompressGZIP);
        return input;
    }


    private void buildImage() throws Throwable {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                getSystemService(SmsManager.class) : SmsManager.getDefault();

        Bitmap imageBitmap = imageHandler.resizeImage(changedResolution);
        imageBitmap = ImageHandler.removeAlpha(imageBitmap);

        compressedBytes = imageHandler.compressImage(COMPRESSION_RATIO, imageBitmap);
        Log.d(getLocalClassName(), "Before ICCP extraction: " + compressedBytes.length);

        compressedBytes = ImageHandler.extractContainerInformation(compressedBytes);
        Log.d(getLocalClassName(), "After ICCP extraction: " + compressedBytes.length);

        Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
        imageView.setImageBitmap(compressedBitmap);

        SecurityECDH securityECDH = new SecurityECDH(getApplicationContext());
        int numberOfmessages = -1;

        String content = ImageHandler.IMAGE_HEADER +
                Base64.encodeToString(compressedBytes, Base64.DEFAULT);

        byte[] c = content.getBytes(StandardCharsets.UTF_8);

        if(securityECDH.hasSecretKey(address)){
            String secretKeyB64 = securityECDH.securelyFetchSecretKey(address);
            c = SecurityECDH.encryptAES(c, Base64.decode(secretKeyB64, Base64.DEFAULT));
            content = Base64.encodeToString(c, Base64.DEFAULT);
            c = SecurityHelpers.putEncryptedMessageWaterMark(content)
                    .getBytes(StandardCharsets.UTF_8);
            Log.d(getLocalClassName(), "Original no compression: " + c.length);
//            c = compress(c);
        }

        numberOfmessages =
                smsManager.divideMessage( Base64.encodeToString(c, Base64.DEFAULT)).size();

        TextView imageResolution = findViewById(R.id.image_details_resolution);
        imageResolution.setText("New resolution: " + imageBitmap.getWidth() + " x " + imageBitmap.getHeight());

        TextView imageSize = findViewById(R.id.image_details_size);
        imageSize.setText("Size " + (compressedBytes.length / 1024) + " KB");

        TextView imageQuality = findViewById(R.id.image_details_quality);
        imageQuality.setText("Quality " + COMPRESSION_RATIO + "%");

        TextView imageSMSCount = findViewById(R.id.image_details_sms_count);
        imageSMSCount.setText(numberOfmessages + " Messages");

    }

    public void sendImage(View view) throws InterruptedException {
        Intent intent = new Intent(this, SMSSendActivity.class);
        intent.putExtra(SMSSendActivity.ADDRESS, address);

        long messageId = Helpers.generateRandomNumber();

        int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());

        String content = ImageHandler.IMAGE_HEADER +
                Base64.encodeToString(compressedBytes, Base64.DEFAULT);

//        content = Base64.encodeToString(compress(content.getBytes(StandardCharsets.UTF_8)),
//                Base64.DEFAULT);

        String threadIdRx = SMSHandler.registerPendingMessage(getApplicationContext(),
                address,
                content,
                messageId,
                subscriptionId);

        intent.putExtra(SMSSendActivity.THREAD_ID, threadIdRx);
        intent.putExtra(SMS_IMAGE_PENDING_LOCATION, messageId);

        startActivity(intent);
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