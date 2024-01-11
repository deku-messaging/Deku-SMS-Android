package com.afkanerd.deku.DefaultSMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Fragments.ArchivedFragments;
import com.afkanerd.deku.DefaultSMS.Fragments.DraftsFragments;
import com.afkanerd.deku.DefaultSMS.Fragments.EncryptionFragments;
import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Fragments.UnreadFragments;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.Router.Router.RouterActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ThreadedConversationsActivity extends CustomAppCompactActivity implements ThreadedConversationsFragment.ViewModelsInterface {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    ActionBar ab;

    HashMap<String, ThreadedConversationRecyclerAdapter> messagesThreadRecyclerAdapterHashMap = new HashMap<>();

    String ITEM_TYPE = "";

    ThreadedConversations threadedConversations = new ThreadedConversations();

    MaterialToolbar toolbar;

    NavigationView navigationView;

    ThreadedConversationsDao threadedConversationsDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_threads);

        toolbar = findViewById(R.id.conversation_threads_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();

        if(!checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        threadedConversationsDao = threadedConversations.getDaoInstance(getApplicationContext());

        threadedConversationsViewModel = new ViewModelProvider(this).get(
                ThreadedConversationsViewModel.class);
        threadedConversationsViewModel.threadedConversationsDao = threadedConversationsDao;


        fragmentManagement();
        configureBroadcastListeners();
        configureNavigationBar();
    }

    public void configureNavigationBar() {
        navigationView = findViewById(R.id.conversations_threads_navigation_view);
        View view = getLayoutInflater().inflate(R.layout.header_navigation_drawer, null);
        TextView textView = view.findViewById(R.id.conversations_threads_navigation_view_version_number);
        textView.setText(BuildConfig.VERSION_NAME);

        navigationView.addHeaderView(view);

        MenuItem inboxMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_inbox);
        MenuItem draftMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_drafts);
        MenuItem encryptedMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_encrypted);
        MenuItem unreadMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_unread);

        threadedConversationsViewModel.folderMetrics.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> integers) {
//                inboxMenuItem.setTitle(getString(R.string.conversations_navigation_view_inbox)
//                        + "(" + integers.get(0) + ")");
                draftMenuItem.setTitle(getString(R.string.conversations_navigation_view_drafts)
                        + "(" + integers.get(1) + ")");

                encryptedMenuItem.setTitle(getString(R.string.homepage_fragment_tab_encrypted)
                        + "(" + integers.get(2) + ")");

                unreadMenuItem.setTitle(getString(R.string.conversations_navigation_view_unread)
                        + "(" + integers.get(3) + ")");
            }
        });

        DrawerLayout drawerLayout = findViewById(R.id.conversations_drawer);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.navigation_view_menu_inbox) {
                    fragmentManagement();
                    drawerLayout.close();
                    return true;
                } else if(item.getItemId() == R.id.navigation_view_menu_drafts) {
                    fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                    DraftsFragments.class, null, "DRAFT_TAG")
                            .setReorderingAllowed(true)
                            .commit();
                    drawerLayout.close();
                    return true;
                } else if(item.getItemId() == R.id.navigation_view_menu_encrypted) {
                    fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                    EncryptionFragments.class, null, "ENCRYPTED_TAG")
                            .setReorderingAllowed(true)
                            .commit();
                    drawerLayout.close();
                    return true;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_unread) {
                    fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                    UnreadFragments.class, null, "UNREAD_TAG")
                            .setReorderingAllowed(true)
                            .commit();
                    drawerLayout.close();
                    return true;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_archive) {
                    fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                    ArchivedFragments.class, null, "ARCHIVED_TAG")
                            .setReorderingAllowed(true)
                            .commit();
                    drawerLayout.close();
                    return true;
                }
                return false;
            }
        });
    }

    private void fragmentManagement() {
        fragmentManager.beginTransaction().replace(R.id.view_fragment,
                        ThreadedConversationsFragment.class, null, "HOMEPAGE_TAG")
                .setReorderingAllowed(true)
                .commit();
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
        Intent intent = new Intent(this, ComposeNewMessageActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.conversations_threads_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.conversation_threads_main_menu_search) {
            Intent searchIntent = new Intent(getApplicationContext(),
                    SearchMessagesThreadsActivity.class);
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(searchIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_archived) {
            Intent archivedIntent = new Intent(getApplicationContext(),
                    ArchivedMessagesActivity.class);
            archivedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(archivedIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_routed) {
            Intent routingIntent = new Intent(getApplicationContext(), RouterActivity.class);
            routingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(routingIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_settings) {
            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_about) {
            Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
            aboutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(aboutIntent);
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
//        setViewModel(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public ThreadedConversationsViewModel getThreadedConversationsViewModel() {
        return threadedConversationsViewModel;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        threadedConversations.close();
    }
}