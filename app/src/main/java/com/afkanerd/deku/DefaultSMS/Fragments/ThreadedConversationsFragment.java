package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.paging.CombinedLoadStates;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afkanerd.deku.DefaultSMS.Models.Archive.ArchiveHandler;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.Objects;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class ThreadedConversationsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    ThreadedConversationsViewModel threadedConversationsViewModel;
    ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    Toolbar toolbar;


    public static final String MESSAGES_THREAD_FRAGMENT_TYPE = "MESSAGES_THREAD_FRAGMENT_TYPE";
    public static final String ALL_MESSAGES_THREAD_FRAGMENT = "ALL_MESSAGES_THREAD_FRAGMENT";
    public static final String PLAIN_MESSAGES_THREAD_FRAGMENT = "PLAIN_MESSAGES_THREAD_FRAGMENT";
    public static final String ENCRYPTED_MESSAGES_THREAD_FRAGMENT = "ENCRYPTED_MESSAGES_THREAD_FRAGMENT";

    public static final String AUTOMATED_MESSAGES_THREAD_FRAGMENT = "AUTOMATED_MESSAGES_THREAD_FRAGMENT";

    private OnViewManipulationListener viewManipulationListener;

    SwipeRefreshLayout swipeRefreshLayout;

    public interface OnViewManipulationListener extends HomepageFragment.TabListenerInterface {
        void activateDefaultToolbar();
        void deactivateDefaultToolbar(int size);

        ThreadedConversationsViewModel getViewModel();

        void setRecyclerViewAdapter(String name, ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter);
        Toolbar getToolbar();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages_threads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String messageType = args.getString(MESSAGES_THREAD_FRAGMENT_TYPE);

        toolbar = viewManipulationListener.getToolbar();

        swipeRefreshLayout = view.findViewById(R.id.conversations_threads_fragment_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        threadedConversationRecyclerAdapter = new ThreadedConversationRecyclerAdapter( getContext());

        viewManipulationListener.setRecyclerViewAdapter(messageType, threadedConversationRecyclerAdapter);

        messagesThreadRecyclerView = view.findViewById(R.id.messages_threads_recycler_view);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        messagesThreadRecyclerView.setAdapter(threadedConversationRecyclerAdapter);
        messagesThreadRecyclerView.setItemViewCacheSize(500);

        threadedConversationsViewModel = viewManipulationListener.getViewModel();
        threadedConversationRecyclerAdapter.addOnPagesUpdatedListener(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                swipeRefreshLayout.setRefreshing(false);
                return null;
            }
        });
        switch(Objects.requireNonNull(messageType)) {
            case ENCRYPTED_MESSAGES_THREAD_FRAGMENT:
                threadedConversationsViewModel.getEncrypted().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case PLAIN_MESSAGES_THREAD_FRAGMENT:
                threadedConversationsViewModel.getNotEncrypted().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case ALL_MESSAGES_THREAD_FRAGMENT:
            default:
                threadedConversationsViewModel.get().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
        }

        threadedConversationRecyclerAdapter.selectedItems.observe(getViewLifecycleOwner(),
                new Observer<Set<ThreadedConversationsTemplateViewHolder>>() {
                    @Override
                    public void onChanged(Set<ThreadedConversationsTemplateViewHolder> stringViewHolderHashMap) {
                        highlightListener(stringViewHolderHashMap.size(), view);
                    }
                });
    }


    private void highlightListener(int size, View view){
        Menu menu = toolbar.getMenu();
        if(size < 1) {
            menu.setGroupVisible(R.id.threads_menu, false);
            viewManipulationListener.activateDefaultToolbar();
        } else {
            viewManipulationListener.deactivateDefaultToolbar(size);
            menu.setGroupVisible(R.id.threads_menu, true);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnViewManipulationListener) {
            viewManipulationListener = (OnViewManipulationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnViewManipulationListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        threadedConversationsViewModel.loadNatives(getContext());
    }

    @Override
    public void onRefresh() {
        threadedConversationsViewModel.loadNatives(getContext());
    }
}
