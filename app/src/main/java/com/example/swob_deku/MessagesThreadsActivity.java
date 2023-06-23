package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.example.swob_deku.Fragments.Homepage.HomepageFragment;
import com.example.swob_deku.Fragments.Homepage.MessagesThreadFragment;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.google.android.material.card.MaterialCardView;

public class MessagesThreadsActivity extends AppCompatActivity implements MessagesThreadFragment.OnViewManipulationListener {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    Toolbar toolbar;
    ActionBar ab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_threads);

        toolbar = findViewById(R.id.messages_threads_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();

        if(!checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
            return;
        }

        loadSubroutines();
        fragmentManagement();
        startServices();
    }

    private void startServices() {
        try {
            GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
            gatewayClientHandler.startServices();
            gatewayClientHandler.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void fragmentManagement() {
        fragmentManager.beginTransaction().replace(R.id.view_fragment,
                        HomepageFragment.class, null, "HOMEPAGE_TAG")
                .setReorderingAllowed(true)
//                .setCustomAnimations(android.R.anim.slide_in_left,
//                        android.R.anim.slide_out_right,
//                        android.R.anim.fade_in,
//                        android.R.anim.fade_out)
                .commit();
    }

    private void loadSubroutines() {
        MaterialCardView cardView = findViewById(R.id.homepage_search_card);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class));
            }
        });

        ImageButton imageButton = findViewById(R.id.homepage_search_image_btn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.messages_threads_menu_item_archived) {
                            Intent archivedIntent = new Intent(getApplicationContext(),
                                    ArchivedMessagesActivity.class);
                            archivedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(archivedIntent);
                            return true;
                        }
                        else if (item.getItemId() == R.id.messages_threads_menu_item_routed) {
                            Intent routingIntent = new Intent(getApplicationContext(), RouterActivity.class);
                            routingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(routingIntent);
                        }
                        else if (item.getItemId() == R.id.messages_threads_settings) {
                            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(settingsIntent);
                        }
                        return false;
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.main_menu, popup.getMenu());
                popup.show();
            }
        });
    }

    private boolean checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancelAll();
    }

    public void onNewMessageClick(View view) {
//        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
//        startActivityForResult(intent, 1);

        Intent intent = new Intent(this, ComposeNewMessageActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.messages_threads_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void activateDefaultToolbar() {
        findViewById(R.id.messages_thread_search_input_constrain).setVisibility(View.VISIBLE);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(null);
    }

    @Override
    public void deactivateDefaultToolbar(int size) {
        findViewById(R.id.messages_thread_search_input_constrain).setVisibility(View.GONE);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
        ab.setTitle(String.valueOf(size));
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }
}