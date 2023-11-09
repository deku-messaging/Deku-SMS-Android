package com.afkanerd.deku.DefaultSMS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Fragments.HomepageFragment;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.Router.Router.RouterActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThreadedConversationsActivity extends CustomAppCompactActivity implements ThreadedConversationsFragment.OnViewManipulationListener {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    Toolbar toolbar;
    ActionBar ab;

    HashMap<String, ThreadedConversationRecyclerAdapter> messagesThreadRecyclerAdapterHashMap = new HashMap<>();
    HashMap<String, ThreadedConversationsViewModel> stringMessagesThreadViewModelHashMap = new HashMap<>();

    String ITEM_TYPE = "";

    ThreadedConversationsViewModel threadedConversationsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_threads);

        ab = getSupportActionBar();

        if(!checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
        }

        ThreadedConversationsDao threadedConversationsDao =
                ThreadedConversations.getDao(getApplicationContext());
        threadedConversationsViewModel = new ViewModelProvider(this).get(
                ThreadedConversationsViewModel.class);
        threadedConversationsViewModel.setThreadedConversationsDao(threadedConversationsDao);

        fragmentManagement();
        startServices();
    }

    private void startServices() {
//        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
//        try {
//            gatewayClientHandler.startServices();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            gatewayClientHandler.close();
//        }

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
//        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
//        startActivityForResult(intent, 1);

        Intent intent = new Intent(this, ComposeNewMessageActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.conversations_threads_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void activateDefaultToolbar() {
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setHomeAsUpIndicator(null);
    }

    @Override
    public void deactivateDefaultToolbar(int size) {
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
        ab.setTitle(String.valueOf(size));
    }

    @Override
    public ThreadedConversationsViewModel getViewModel() {
        return threadedConversationsViewModel;
    }

    @Override
    public void setRecyclerViewAdapter(String itemType, ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter) {
        this.ITEM_TYPE = itemType;
        this.messagesThreadRecyclerAdapterHashMap.put(itemType, threadedConversationRecyclerAdapter);
//        this.threadedConversationRecyclerAdapter = threadedConversationRecyclerAdapter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(getLocalClassName(), "Item clicked: " + item.getItemId());
        if (item.getItemId() == android.R.id.home &&
                this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE) != null &&
                this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems != null &&
                this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems.getValue() != null) {
            this.messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).resetAllSelectedItems();
            return true;
        }
        if(item.getItemId() == R.id.conversations_threads_main_menu_delete) {
            List<ThreadedConversations> threadedConversations = new ArrayList<>();
            ThreadedConversationRecyclerAdapter recyclerAdapter =
                    messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE);
            if(recyclerAdapter != null && recyclerAdapter.selectedItems != null &&
                    recyclerAdapter.selectedItems.getValue() != null) {
                for (ThreadedConversationsTemplateViewHolder viewHolder :
                        recyclerAdapter.selectedItems.getValue()) {
                    ThreadedConversations threadedConversation = new ThreadedConversations();
                    threadedConversation.setThread_id(viewHolder.id);
                    threadedConversations.add(threadedConversation);
                }
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        recyclerAdapter.resetAllSelectedItems();
                        threadedConversationsViewModel.delete(getApplicationContext(),
                                threadedConversations);
                    }
                };
                showAlert(runnable);
            }
            return true;
        }

        if(item.getItemId() == R.id.conversations_threads_main_menu_archive) {
            List<Archive> archiveList = new ArrayList<>();
            for(ThreadedConversationsTemplateViewHolder templateViewHolder :
                    messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).selectedItems.getValue()) {
                Archive archive = new Archive();
                archive.thread_id = templateViewHolder.id;
                archive.is_archived = true;
                archiveList.add(archive);
            }
            threadedConversationsViewModel.archive(archiveList);
            messagesThreadRecyclerAdapterHashMap.get(ITEM_TYPE).resetAllSelectedItems();
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
        if (item.getItemId() == R.id.conversation_threads_main_menu_devices) {
            Intent webIntent = new Intent(getApplicationContext(), LinkedDevicesQRActivity.class);
            webIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(webIntent);
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void tabUnselected(int position) {
        String itemType = HomepageFragment.HomepageFragmentAdapter.fragmentList[position];
        if(this.messagesThreadRecyclerAdapterHashMap.get(itemType) != null &&
                this.messagesThreadRecyclerAdapterHashMap.get(itemType) != null &&
                this.messagesThreadRecyclerAdapterHashMap.get(itemType).selectedItems.getValue() != null) {

            this.messagesThreadRecyclerAdapterHashMap.get(itemType).resetAllSelectedItems();
        }
    }

    @Override
    public void tabSelected(int position) {
        this.ITEM_TYPE = HomepageFragment.HomepageFragmentAdapter.fragmentList[position];
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}