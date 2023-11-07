package com.afkanerd.deku.DefaultSMS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ArchivedViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;

import java.util.Set;

public class ArchivedMessagesActivity extends AppCompatActivity {

    public ThreadedConversationRecyclerAdapter archivedThreadRecyclerAdapter;

    ArchivedViewModel archivedViewModel;
    Toolbar myToolbar;
    ActionBar ab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_messages);

        myToolbar = (Toolbar) findViewById(R.id.messages_archived_toolbar);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ab = getSupportActionBar();
        ab.setTitle(R.string.archived_messages_toolbar_title);

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);


        RecyclerView archivedMessagesRecyclerView = findViewById(R.id.messages_archived_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        archivedMessagesRecyclerView.setLayoutManager(linearLayoutManager);

        archivedThreadRecyclerAdapter = new ThreadedConversationRecyclerAdapter(this);

        archivedMessagesRecyclerView.setAdapter(archivedThreadRecyclerAdapter);

        archivedViewModel = new ViewModelProvider(this).get(
                ArchivedViewModel.class);

        ThreadedConversationsDao threadedConversationsDao =
                ThreadedConversations.getDao(getApplicationContext());
        archivedViewModel.get(threadedConversationsDao).observe(this,
                new Observer<PagingData<ThreadedConversations>>() {
                    @Override
                    public void onChanged(PagingData<ThreadedConversations> smsList) {
                        archivedThreadRecyclerAdapter.submitData(getLifecycle(), smsList);
                        findViewById(R.id.messages_archived_no_messages).setVisibility(View.GONE);
                    }
                });

        archivedThreadRecyclerAdapter.selectedItems.observe(this, new Observer<Set<ThreadedConversationsTemplateViewHolder>>() {
            @Override
            public void onChanged(Set<ThreadedConversationsTemplateViewHolder> stringViewHolderHashMap) {
                highlightListener(stringViewHolderHashMap.size());
            }
        });

        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.archive_unarchive) {
                    archivedThreadRecyclerAdapter.resetAllSelectedItems();
                }
                else if(item.getItemId() == R.id.archive_delete) {
                    archivedThreadRecyclerAdapter.resetAllSelectedItems();
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && archivedThreadRecyclerAdapter.selectedItems.getValue() != null &&
                !archivedThreadRecyclerAdapter.selectedItems.getValue().isEmpty()) {
            archivedThreadRecyclerAdapter.resetAllSelectedItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.archive_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void highlightListener(int size){
        Menu menu = myToolbar.getMenu();
        if(size < 1) {
            menu.setGroupVisible(R.id.archive_menu, false);
            ab.setTitle(R.string.archived_messages_toolbar_title);
            ab.setHomeAsUpIndicator(null);
        } else {
            menu.setGroupVisible(R.id.archive_menu, true);
            ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
            ab.setTitle(String.valueOf(size));
        }
    }
}