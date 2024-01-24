package com.afkanerd.deku.DefaultSMS.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afkanerd.deku.DefaultSMS.R;

public class UnreadFragments extends  ThreadedConversationsFragment {
    public UnreadFragments() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = new Bundle();
        bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE,
                UNREAD_MESSAGE_TYPES);
        super.setArguments(bundle);
        defaultMenu = R.menu.read_menu;

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLabels(view, getString(R.string.conversations_navigation_view_unread),
                getString(R.string.homepage_unread_no_message));
    }
}
