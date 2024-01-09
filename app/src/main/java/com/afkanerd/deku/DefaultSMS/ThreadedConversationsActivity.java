package com.afkanerd.deku.DefaultSMS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
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
import com.afkanerd.deku.DefaultSMS.Fragments.DraftsFragments;
import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Fragments.HomepageFragment;
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

public class ThreadedConversationsActivity extends CustomAppCompactActivity implements ThreadedConversationsFragment.OnViewManipulationListener {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    ActionBar ab;

    HashMap<String, ThreadedConversationRecyclerAdapter> messagesThreadRecyclerAdapterHashMap = new HashMap<>();

    String ITEM_TYPE = "";

    ActionMode actionMode;
    ThreadedConversationsDao threadedConversationsDao;
    ThreadedConversations threadedConversations = new ThreadedConversations();

    MaterialToolbar toolbar;
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
        threadedConversationsViewModel.setThreadedConversationsDao(threadedConversationsDao);
        fragmentManagement();
        configureBroadcastListeners();
        configureNavigationBar();
    }

    public void configureNavigationBar() {
        NavigationView navigationView = findViewById(R.id.conversations_threads_navigation_view);
        View view = getLayoutInflater().inflate(R.layout.header_navigation_drawer, null);
        TextView textView = view.findViewById(R.id.conversations_threads_navigation_view_version_number);
        textView.setText(BuildConfig.VERSION_NAME);

        navigationView.addHeaderView(view);

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
                fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                DraftsFragments.class, null, "DRAFT_TAG")
//                        .setReorderingAllowed(true)
                        .commit();
                item.setChecked(true);
                drawerLayout.close();
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        conversation.close();
    }

    private void fragmentManagement() {
        fragmentManager.beginTransaction().replace(R.id.view_fragment,
                        HomepageFragment.class, null, "HOMEPAGE_TAG")
                .setReorderingAllowed(true)
                .commit();
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
    public ThreadedConversationsViewModel getViewModel() {
        return threadedConversationsViewModel;
    }

    @Override
    public void setRecyclerViewAdapter(String itemType, ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter) {
        this.ITEM_TYPE = itemType;
        this.messagesThreadRecyclerAdapterHashMap.put(itemType, threadedConversationRecyclerAdapter);

        this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems.observe(this,
                new Observer<HashMap<Long, ThreadedConversationsTemplateViewHolder>>() {
            @Override
            public void onChanged(HashMap<Long, ThreadedConversationsTemplateViewHolder>
                                          threadedConversationsTemplateViewHolders) {
                if(threadedConversationsTemplateViewHolders == null ||
                        threadedConversationsTemplateViewHolders.isEmpty()) {
                    if(actionMode != null) {
                        actionMode.finish();
                    }
                    return;
                }
                else if(actionMode == null) {
                    actionMode = startActionMode(actionModeCallback);
                }
                if(actionMode != null)
                    actionMode
                            .setTitle(String.valueOf(threadedConversationsTemplateViewHolders.size()));

            }
        });
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
    public void tabUnselected(int position) {
        if(actionMode != null)
            actionMode.finish();
    }

    @Override
    public void tabSelected(int position) {
        this.ITEM_TYPE = HomepageFragment.fragmentList.get(position);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        setViewModel(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPreferences.getBoolean(LOAD_NATIVES, true) ) {
            sharedPreferences.edit().putBoolean(LOAD_NATIVES, false).apply();
            threadedConversationsViewModel.reset(getApplicationContext());
        }

        threadedConversationsViewModel.refresh(getApplicationContext());
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Objects.requireNonNull(getSupportActionBar()).hide();
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.conversations_threads_menu_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final ThreadedConversationRecyclerAdapter recyclerAdapter =
                    messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE);
            if(recyclerAdapter != null) {
                if(item.getItemId() == R.id.conversations_threads_main_menu_delete) {
                    if(recyclerAdapter.selectedItems != null && recyclerAdapter.selectedItems.getValue() != null) {
                        List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                        List<String> ids = new ArrayList<>();
                        for (ThreadedConversationsTemplateViewHolder viewHolder :
                                recyclerAdapter.selectedItems.getValue().values()) {
                            ThreadedConversations threadedConversation = new ThreadedConversations();
                            threadedConversation.setThread_id(viewHolder.id);
                            threadedConversationsList.add(threadedConversation);
                            ids.add(threadedConversation.getThread_id());
                        }
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                recyclerAdapter.resetAllSelectedItems();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        List<ThreadedConversations> foundList =
                                                threadedConversationsDao.find(ids);
                                        for(ThreadedConversations threadedConversation :
                                                foundList) {
                                            try {
                                                String keystoreAlias =
                                                        E2EEHandler.deriveKeystoreAlias(
                                                                threadedConversation.getAddress(),
                                                                0);
                                                E2EEHandler.removeFromKeystore(
                                                        getApplicationContext(), keystoreAlias);
                                                E2EEHandler.removeFromEncryptionDatabase(
                                                        getApplicationContext(), keystoreAlias);
                                            } catch (KeyStoreException | NumberParseException |
                                                     InterruptedException | NoSuchAlgorithmException | IOException |
                                                     CertificateException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        threadedConversationsViewModel.delete(getApplicationContext(),
                                                threadedConversationsList);
                                    }
                                }).start();
                            }
                        };
                        showAlert(runnable);
                    }
                    return true;
                }

                if(item.getItemId() == R.id.conversations_threads_main_menu_archive) {
                    List<Archive> archiveList = new ArrayList<>();
                    for(ThreadedConversationsTemplateViewHolder templateViewHolder :
                            recyclerAdapter.selectedItems.getValue().values()) {
                        Archive archive = new Archive();
                        archive.thread_id = templateViewHolder.id;
                        archive.is_archived = true;
                        archiveList.add(archive);
                    }
                    threadedConversationsViewModel.archive(archiveList);
                    recyclerAdapter.resetAllSelectedItems();
                    return true;
                }

            }
            return false;
        }

        // Called when the user exits the action mode.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Objects.requireNonNull(getSupportActionBar()).show();
            actionMode = null;
            final ThreadedConversationRecyclerAdapter recyclerAdapter =
                    messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE);
            if(recyclerAdapter != null)
                recyclerAdapter.resetAllSelectedItems();
        }
    };

//    private void loadConversationsFromNative(Context context) {
//        Cursor cursor = NativeSMSDB.fetchAll(context);
//        List<Conversation> conversationList = new ArrayList<>();
//        if(cursor.moveToNext()) {
//            do {
//                conversationList.add(Conversation.build(cursor));
//            } while(cursor.moveToNext());
//        }
//        cursor.close();
//        ConversationDao conversationDao = conversation.getDaoInstance(context);
//        conversationDao.insertAll(conversationList);
//    }
}