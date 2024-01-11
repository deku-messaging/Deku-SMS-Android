package com.afkanerd.deku.DefaultSMS.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afkanerd.deku.DefaultSMS.R;

public class ArchivedFragments extends ThreadedConversationsFragment {

    public ArchivedFragments() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = new Bundle();
        bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE, ARCHIVED_MESSAGE_TYPES);
        super.setArguments(bundle);
        return super.onCreateView(inflater, container, savedInstanceState);
//        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLabels(view, getString(R.string.conversations_navigation_view_archived),
                getString(R.string.homepage_archive_no_message));
    }

}
