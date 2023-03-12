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
import java.util.ArrayList;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;

    TextView imageDescription;

    Bitmap compressedBitmap;
    byte[] compressedBytes;

    ArrayList<String> concatenatedSegments = new ArrayList<>();

    String address = "";
    String threadId = "";

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
        imageDescription = findViewById(R.id.image_details);

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
                buildImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buildImage(byte[] data ) throws IOException {
        compressedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
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

        ArrayList<String> dividedArray = smsManager.divideMessage(
                Base64.encodeToString(compressedBytes, Base64.DEFAULT));

        concatenatedSegments = ImageHandler.concatenateMessages(dividedArray, 5);

        Log.d(getLocalClassName(), "Image concat messages size: " + concatenatedSegments.get(0));
        Log.d(getLocalClassName(), "Image concat messages size: " + concatenatedSegments.get(1));
        Log.d(getLocalClassName(), "Image concat messages size: " + concatenatedSegments.get(2));

        description += "\n\n- Compressed bytes: " + compressedBytes.length;
        description += "\n- Approx # B64 SMS: " + dividedArray.size();
        description += "\n- Concatenated B64 SMS: " + concatenatedSegments.size() + " segments";
        description += "\n- Approx # Data SMS: " + SMSHandler.structureSMSMessage(compressedBytes).size();

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

        if(!threadId.isEmpty())
            intent.putExtra(SMSSendActivity.THREAD_ID, threadId);

        startActivity(intent);

//        SMSHandler.sendDataSMS(getApplicationContext(), address, compressedBytes,
//                null, null, -1);


//        long[] messageIds = new long[concatenatedSegments.size()];
//        for(int i=0;i<concatenatedSegments.size();++i) {
////            messageIds[i] = Helpers.generateRandomNumber();
//            messageIds[i] = SMSHandler.generateSmsId(getApplicationContext());
//            SMSHandler.registerPendingMessage(getApplicationContext(), address,
//                    concatenatedSegments.get(i), messageIds[i]);
//        }
//        /**
//         * - Register segments
//         * - pass registered segments to workmanager
//         */
//        SMSHandler.createWorkManagersForDataMessages(getApplicationContext(), address, messageIds);
        SMSHandler.sendTextSMS(getApplicationContext(), address,
                Base64.encodeToString(compressedBytes, Base64.DEFAULT), null, null,
                Helpers.generateRandomNumber());
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