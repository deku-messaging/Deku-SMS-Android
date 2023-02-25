package com.example.swob_deku;

import static com.example.swob_deku.SMSSendActivity.ID;
import static com.example.swob_deku.SMSSendActivity.SMS_DELIVERED_INTENT;
import static com.example.swob_deku.SMSSendActivity.SMS_SENT_INTENT;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.io.IOException;
import java.util.Arrays;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;

    TextView imageDescription;

    Bitmap compressedBitmap;
    byte[] compressedBytes;

    String address;
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

        address = getIntent().getStringExtra(SMSSendActivity.ADDRESS);
        imageUri = Uri.parse(getIntent().getStringExtra(SMSSendActivity.IMAGE_URI));
        imageView = findViewById(R.id.compressed_image_holder);
        imageDescription = findViewById(R.id.image_details);

        try {
            buildImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildImage() throws IOException {
        // TODO: messages >40 trigger large message warning...
        ImageHandler imageHandler = new ImageHandler(getApplicationContext(), imageUri);

        final int SCALE_DOWN_RATIO = 3;
        final int COMPRESSION_RATIO = 0;

        int resizeScale = imageHandler.bitmap.getWidth() / SCALE_DOWN_RATIO;

        Bitmap imageBitmap = imageHandler.resizeImage(resizeScale);

        String description = "- Original resolution: " + imageHandler.bitmap.getWidth() + "x"
                + imageHandler.bitmap.getHeight();
        description += "\n\n- Resize scale: " + resizeScale;
        description += "\n- Resize resolution: " + imageBitmap.getWidth()
                + "x" + imageBitmap.getHeight();
        compressedBytes = imageHandler.compressImage(COMPRESSION_RATIO, imageBitmap);
        description += "\n\n- Compressed bytes: " + compressedBytes.length;
        description += "\n- Approx #SMS: " + SMSHandler.divideMessage(compressedBytes).size();

        Log.d(getLocalClassName(), "Google loc RIFF: " + DataHelper.findInBytes("RIFF", compressedBytes));
        Log.d(getLocalClassName(), "Google loc Google: " + DataHelper.findInBytes("Google", compressedBytes));
        Log.d(getLocalClassName(), "Google loc VP8: " + DataHelper.findInBytes("VP8", compressedBytes));
        Log.d(getLocalClassName(), "Google loc enUS: " + DataHelper.findInBytes("enUS", compressedBytes));
        Log.d(getLocalClassName(), "Google loc desc: " + DataHelper.findInBytes("desc", compressedBytes));
        Log.d(getLocalClassName(), "Google loc 2016: " + DataHelper.findInBytes("2016", compressedBytes));

        byte[] riffHeader = SMSHandler.copyBytes(compressedBytes, 0, 12);
        byte[] vp8Header = SMSHandler.copyBytes(compressedBytes, 12, 4);

        Log.d(getLocalClassName(), "Header RIFF: " + Arrays.toString(DataHelper.byteToChar(riffHeader)));
        Log.d(getLocalClassName(), "Header VP8: " + Arrays.toString(DataHelper.byteToChar(vp8Header)));

        int locEnUS = DataHelper.findInBytes("enUS", compressedBytes);
        int stdLen = 200;
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

        compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);

        imageDescription.setText(description);
        imageView.setImageBitmap(compressedBitmap);
    }

    public void imageSend(View view) {
        Intent intent = new Intent(this, SMSSendActivity.class);
        intent.putExtra(SMSSendActivity.COMPRESSED_IMAGE_BYTES, compressedBytes);
        intent.putExtra(SMSSendActivity.ADDRESS, address);

        startActivity(intent);
        finish();
    }

}