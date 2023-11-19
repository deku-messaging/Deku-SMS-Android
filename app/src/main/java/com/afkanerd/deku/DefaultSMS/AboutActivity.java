package com.afkanerd.deku.DefaultSMS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.afkanerd.deku.DefaultSMS.BuildConfig;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        ab.setTitle(getString(R.string.about_deku));
        ab.setDisplayHomeAsUpEnabled(true);

        setVersion();

        setClickListeners();
    }

    private void setVersion() {
        TextView textView = findViewById(R.id.about_version_text);
        textView.setText(BuildConfig.VERSION_NAME);
    }

    private void setClickListeners() {
        TextView textView = findViewById(R.id.about_github_link);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = getString(R.string.about_deku_github_url);
                Intent shareIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(shareIntent);
            }
        });
    }
}