package com.afkanerd.deku.DefaultSMS.Fragments;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.ArrayList;
import java.util.List;

public class ArchivedFragments extends ThreadedConversationsFragment {

    public ArchivedFragments() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Bundle bundle = new Bundle();
        bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE, ARCHIVED_MESSAGE_TYPES);

        super.setArguments(bundle);
        actionModeMenu = R.menu.archive_menu_items_selected;
        defaultMenu = R.menu.archive_menu;

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLabels(view, getString(R.string.conversations_navigation_view_archived),
                getString(R.string.homepage_archive_no_message));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.archive_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
