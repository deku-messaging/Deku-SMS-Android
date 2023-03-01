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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;

    TextView imageDescription;

    Bitmap compressedBitmap;
    byte[] compressedBytes;

    String address = "";
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
        imageDescription = findViewById(R.id.image_details);

        if(getIntent().hasExtra("image_sms_id")) {
            String smsId = getIntent().getStringExtra("image_sms_id");

            // TODO: Get all messages which have the Ref ID
            // TODO: get until the len of messages have been acquired, then fit them together
            // TODO: until the len has been acquired.

            Cursor cursor = SMSHandler.fetchSMSInboxById(getApplicationContext(), smsId);
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);

                byte[] body = Base64.decode(sms.getBody(), Base64.NO_PADDING);
                if(body.length <= 133) {
                    int len = Byte.toUnsignedInt(body[2]);

                    // TODO: build for len so not to waste compute
                    String RIL = Base64.encodeToString(new byte[]{body[0]}, Base64.NO_PADDING)
                            .replaceAll("\\n", "");

                    //                String RIL = "vg";
                    Log.d(getLocalClassName(), "Image Header RIL: " + RIL + ":" + RIL.length());
                    Cursor cursorImageCursor = SMSHandler.fetchSMSInboxByForImages(getApplicationContext(),
                            RIL, sms.getThreadId());
                    Log.d(getLocalClassName(), "Image # Found: " + cursorImageCursor.getCount() + ":" + len);

                    byte[][] imagesBytes = new byte[len][];

                    if (cursorImageCursor.moveToFirst()) {
                        do {
                            SMS imageSMS = new SMS(cursorImageCursor);

                            byte[] imgBody = Base64.decode(imageSMS.getBody(), Base64.NO_PADDING);
                            int id = Byte.toUnsignedInt(imgBody[1]);
                            imagesBytes[id] = imgBody;
                        } while (cursorImageCursor.moveToNext());
                    }
                    try {
                        buildImage(imagesBytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    cursorImageCursor.close();
                }
                else {
                    try {
                        buildImage(body);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            cursor.close();
        }
        else {
            address = getIntent().getStringExtra(SMSSendActivity.ADDRESS);
            imageUri = Uri.parse(getIntent().getStringExtra(SMSSendActivity.IMAGE_URI));

            try {
                buildImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buildImage(byte[] data ) throws IOException {
        compressedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//        imageDescription.setText(description);
        imageView.setImageBitmap(compressedBitmap);
    }

    private void buildImage(byte[][] unstructuredImageBytes ) throws IOException {
        byte[] structuredImageBytes = SMSHandler.rebuildStructuredSMSMessage(unstructuredImageBytes);
        Log.d(getLocalClassName(), "Received divide: " + structuredImageBytes.length);

        compressedBitmap = BitmapFactory.decodeByteArray(structuredImageBytes, 0,
                structuredImageBytes.length);

//        imageDescription.setText(description);
        imageView.setImageBitmap(compressedBitmap);
    }

    private void buildImage() throws IOException {
        // TODO: messages >40 trigger large message warning...
        ImageHandler imageHandler = new ImageHandler(getApplicationContext(), imageUri);

        final int SCALE_DOWN_RATIO = 3;
        final int COMPRESSION_RATIO = 0;

        int resizeScale = imageHandler.bitmap.getWidth() / SCALE_DOWN_RATIO;

        Bitmap imageBitmap = imageHandler.resizeImage(resizeScale);

        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                getSystemService(SmsManager.class) : SmsManager.getDefault();

        String description = "- Original resolution: " + imageHandler.bitmap.getWidth() + "x"
                + imageHandler.bitmap.getHeight();
        description += "\n\n- Resize scale: " + resizeScale;
        description += "\n- Resize resolution: " + imageBitmap.getWidth()
                + "x" + imageBitmap.getHeight();
        compressedBytes = imageHandler.compressImage(COMPRESSION_RATIO, imageBitmap);
        description += "\n\n- Compressed bytes: " + compressedBytes.length;
        description += "\n- Approx # Data SMS: " + SMSHandler.structureSMSMessage(compressedBytes).size();
        description += "\n- Approx # B64 SMS: " + smsManager.divideMessage(
                Base64.encodeToString(compressedBytes, Base64.NO_PADDING)).size();

        byte[] riffHeader = SMSHandler.copyBytes(compressedBytes, 0, 12);
        byte[] vp8Header = SMSHandler.copyBytes(compressedBytes, 12, 4);

        int locEnUS = DataHelper.findInBytes("enUS", compressedBytes);
        byte[] deepsearchByte = SMSHandler.copyBytes(compressedBytes, locEnUS, 400) ;
        char[] deepsearch = DataHelper.byteToChar(deepsearchByte);

        for(int i=0;i<deepsearch.length;++i) {
            Log.d(getLocalClassName(), "image loc: "
                    + (i + locEnUS) + " - "
                    + deepsearchByte[i] + " - "  + deepsearch[i]);
        }

        char[] header =
                DataHelper.byteToChar(SMSHandler.copyBytes(compressedBytes, locEnUS, 32));

        description += "\n- Headers: \n";
        for(int i=0;i<header.length; ++i)
            Log.d(getLocalClassName(), "image meta:" + i + ": " + header[i]);

        ArrayList<byte[]> structuredMessage = SMSHandler.structureSMSMessage(compressedBytes);

        byte[][] unstructuredImageBytes = new byte[structuredMessage.size()][];

        for(int i=0;i<structuredMessage.size();++i)
            unstructuredImageBytes[i] = structuredMessage.get(i);

        Log.d(getLocalClassName(), "Before structure: " + compressedBytes.length);
        byte[] unstructuredMessage = SMSHandler.rebuildStructuredSMSMessage(unstructuredImageBytes);
        Log.d(getLocalClassName(), "After structure: " + unstructuredMessage.length);

        compressedBitmap = BitmapFactory.decodeByteArray(unstructuredMessage, 0, unstructuredMessage.length);
        imageDescription.setText(description);
        imageView.setImageBitmap(compressedBitmap);
    }

    public void sendImage(View view) throws InterruptedException {
        Intent intent = new Intent(this, SMSSendActivity.class);
        intent.putExtra(SMSSendActivity.ADDRESS, address);

        startActivity(intent);

        long messageId = Helpers.generateRandomNumber();
        SMSHandler.sendDataSMS(getApplicationContext(), address, compressedBytes,
                null, null, messageId);
        finish();
    }

}