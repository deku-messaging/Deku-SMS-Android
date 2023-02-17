package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.swob_deku.Models.Images.ImageHandler;

import java.io.IOException;

public class ImageViewActivity extends AppCompatActivity {

    Uri imageUri;
    ImageView imageView;
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

        imageUri = Uri.parse(getIntent().getStringExtra("image_uri"));
        imageView = findViewById(R.id.compressed_image_holder);

        try {
            buildImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildImage() throws IOException {
        ImageHandler imageHandler = new ImageHandler(getApplicationContext(), imageUri);

        imageView.setImageBitmap(imageHandler.bitmap);
    }
}