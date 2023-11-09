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
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ArchivedViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ArchivedMessagesActivity extends AppCompatActivity {

    public ThreadedConversationRecyclerAdapter archivedThreadRecyclerAdapter;

    ArchivedViewModel archivedViewModel;

    ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_messages);

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

        archivedThreadRecyclerAdapter.selectedItems.observe(this, new Observer<HashMap<Long, ThreadedConversationsTemplateViewHolder>>() {
            @Override
            public void onChanged(HashMap<Long, ThreadedConversationsTemplateViewHolder> stringViewHolderHashMap) {
                if(stringViewHolderHashMap == null || stringViewHolderHashMap.isEmpty()) {
                    if(actionMode != null) {
                        actionMode.finish();
                    }
                    return;
                }
                else if(actionMode == null) {
                    actionMode = startActionMode(actionModeCallback);
                }
                if(actionMode != null)
                    actionMode .setTitle(String.valueOf(stringViewHolderHashMap.size()));

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.archive_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.archive_menu_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(item.getItemId() == R.id.archive_unarchive) {
                List<Archive> archiveList = new ArrayList<>();
                if(archivedThreadRecyclerAdapter.selectedItems != null &&
                        archivedThreadRecyclerAdapter.selectedItems.getValue() != null)
                    for(ThreadedConversationsTemplateViewHolder viewHolder :
                            archivedThreadRecyclerAdapter.selectedItems.getValue().values()) {
                        Archive archive = new Archive();
                        archive.thread_id = viewHolder.id;
                        archive.is_archived = false;
                        archiveList.add(archive);
                    }
                archivedViewModel.unarchive(archiveList);
                archivedThreadRecyclerAdapter.resetAllSelectedItems();
            }
            else if(item.getItemId() == R.id.archive_delete) {
                List<ThreadedConversations> threadedConversations = new ArrayList<>();
                if(archivedThreadRecyclerAdapter.selectedItems != null &&
                        archivedThreadRecyclerAdapter.selectedItems.getValue() != null)
                    for(ThreadedConversationsTemplateViewHolder viewHolder :
                            archivedThreadRecyclerAdapter.selectedItems.getValue().values()) {
                        ThreadedConversations threadedConversation = new ThreadedConversations();
                        threadedConversation.setThread_id(viewHolder.id);
                        threadedConversations.add(threadedConversation);
                    }
                archivedViewModel.delete(getApplicationContext(), threadedConversations);
                archivedThreadRecyclerAdapter.resetAllSelectedItems();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            archivedThreadRecyclerAdapter.resetAllSelectedItems();
        }
    };

}