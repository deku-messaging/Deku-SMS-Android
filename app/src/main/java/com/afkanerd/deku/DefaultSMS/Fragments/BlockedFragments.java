package com.afkanerd.deku.DefaultSMS.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afkanerd.deku.DefaultSMS.R;

public class BlockedFragments extends ThreadedConversationsFragment{

    public BlockedFragments() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Bundle bundle = new Bundle();
        bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE,
                BLOCKED_MESSAGE_TYPES);

        super.setArguments(bundle);
        actionModeMenu = R.menu.blocked_conversations_items_selected;
        defaultMenu = R.menu.blocked_conversations;

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLabels(view, getString(R.string.conversation_menu_block),
                getString(R.string.homepage_blocked_no_message));
    }
}
