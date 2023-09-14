package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;

import android.content.DialogInterface;
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
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.CustomAppCompactActivity;
import com.example.swob_deku.Models.GatewayClients.GatewayClientHandler;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

public class MessagesThreadsActivity extends CustomAppCompactActivity implements MessagesThreadFragment.OnViewManipulationListener {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    Toolbar toolbar;
    ActionBar ab;

    HashMap<String, MessagesThreadRecyclerAdapter> messagesThreadRecyclerAdapterHashMap = new HashMap<>();
    HashMap<String, MessagesThreadViewModel> stringMessagesThreadViewModelHashMap = new HashMap<>();

    String ITEM_TYPE = "";

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

        configureToolbarEvents();
        loadSubroutines();
        fragmentManagement();
        startServices();
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
                        else if (item.getItemId() == R.id.messages_threads_menu_item_web) {
                            Intent webIntent = new Intent(getApplicationContext(), LinkedDevicesActivity.class);
                            webIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(webIntent);
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

    private void showAlert(Runnable runnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.messages_thread_delete_confirmation_title));
        builder.setMessage(getString(R.string.messages_thread_delete_confirmation_text));

        builder.setPositiveButton(getString(R.string.messages_thread_delete_confirmation_yes),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runnable.run();
            }
        });

        builder.setNegativeButton(getString(R.string.messages_thread_delete_confirmation_cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void configureToolbarEvents() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String[] ids = messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems.getValue()
                        .keySet().toArray(new String[0]);
                if(item.getItemId() == R.id.threads_delete) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SecurityECDH securityECDH = new SecurityECDH(getApplicationContext());
                                for(String id : ids) {
                                    SMS.SMSMetaEntity smsMetaEntity = new SMS.SMSMetaEntity();
                                    smsMetaEntity.setThreadId(getApplicationContext(), id);
                                    securityECDH.removeAllKeys(smsMetaEntity.getAddress());
                                }
                                SMSHandler.deleteThreads(getApplicationContext(), ids);
                                messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).resetAllSelectedItems();
                                stringMessagesThreadViewModelHashMap.get(ITEM_TYPE).informChanges(getApplicationContext());
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    showAlert(runnable);
                }
                else if(item.getItemId() == R.id.threads_archive) {
                    long[] longArr = new long[ids.length];
                    for (int i = 0; i < ids.length; i++)
                        longArr[i] = Long.parseLong(ids[i]);

                    try {
                        ArchiveHandler archiveHandler = new ArchiveHandler(getApplicationContext());
                        archiveHandler.archiveMultipleSMS(getApplicationContext(), longArr);
                        messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).resetAllSelectedItems();
                        stringMessagesThreadViewModelHashMap.get(ITEM_TYPE).informChanges(getApplicationContext());
                        archiveHandler.close();
                        return true;
                    } catch (InterruptedException | GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
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
        ab.setDisplayHomeAsUpEnabled(false);
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
    public void setRecyclerViewAdapter(String itemType, MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter) {
        this.ITEM_TYPE = itemType;
        this.messagesThreadRecyclerAdapterHashMap.put(itemType, messagesThreadRecyclerAdapter);
//        this.messagesThreadRecyclerAdapter = messagesThreadRecyclerAdapter;
    }

    @Override
    public void setViewModel(String itemType, MessagesThreadViewModel messagesThreadViewModel) {
        this.ITEM_TYPE = itemType;
        this.stringMessagesThreadViewModelHashMap.put(itemType, messagesThreadViewModel);
        configureBroadcastListeners(new Runnable() {
            @Override
            public void run() {
                try {
                    messagesThreadViewModel.informChanges(getApplicationContext());
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems.getValue() != null) {
            this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).resetAllSelectedItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public void tabUnselected(int position) {
        String itemType = HomepageFragment.HomepageFragmentAdapter.fragmentList[position];
        if(this.messagesThreadRecyclerAdapterHashMap.get(itemType) != null &&
                this.messagesThreadRecyclerAdapterHashMap.get(itemType).selectedItems.getValue() != null) {
            this.messagesThreadRecyclerAdapterHashMap.get(itemType).resetAllSelectedItems();
        }
    }

    @Override
    public void tabSelected(int position) {
        this.ITEM_TYPE = HomepageFragment.HomepageFragmentAdapter.fragmentList[position];
    }
}