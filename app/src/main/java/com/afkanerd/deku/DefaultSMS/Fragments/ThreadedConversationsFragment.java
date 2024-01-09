package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ThreadedConversationsFragment extends Fragment {

    ThreadedConversationsViewModel threadedConversationsViewModel;
    ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    public static final String MESSAGES_THREAD_FRAGMENT_TYPE = "MESSAGES_THREAD_FRAGMENT_TYPE";
    public static final String ALL_MESSAGES_THREAD_FRAGMENT = "ALL_MESSAGES_THREAD_FRAGMENT";
    public static final String PLAIN_MESSAGES_THREAD_FRAGMENT = "PLAIN_MESSAGES_THREAD_FRAGMENT";
    public static final String ENCRYPTED_MESSAGES_THREAD_FRAGMENT = "ENCRYPTED_MESSAGES_THREAD_FRAGMENT";

    public static final String DRAFTS_MESSAGE_TYPES = "DRAFTS_MESSAGE_TYPES";

    public static final String AUTOMATED_MESSAGES_THREAD_FRAGMENT = "AUTOMATED_MESSAGES_THREAD_FRAGMENT";

    private OnViewManipulationListener viewManipulationListener;

    public interface OnViewManipulationListener extends HomepageFragment.TabListenerInterface {
        ThreadedConversationsViewModel getViewModel();

        void setRecyclerViewAdapter(String name, ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversations_threads, container, false);
    }

    public void setLabels(View view, String label, String noContent) {
        ((TextView) view.findViewById(R.id.conversation_threads_fragment_label))
                .setText(label);
        ((TextView) view.findViewById(R.id.homepage_no_message))
                .setText(noContent);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String messageType = args == null ? ALL_MESSAGES_THREAD_FRAGMENT :
                args.getString(MESSAGES_THREAD_FRAGMENT_TYPE);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);

        setLabels(view, getString(R.string.conversations_navigation_view_inbox),
                getString(R.string.homepage_no_message));

        threadedConversationsViewModel = viewManipulationListener.getViewModel();
        threadedConversationRecyclerAdapter = new ThreadedConversationRecyclerAdapter( getContext(),
                threadedConversationsViewModel.getThreadedConversationsDao());

        viewManipulationListener.setRecyclerViewAdapter(messageType, threadedConversationRecyclerAdapter);

        messagesThreadRecyclerView = view.findViewById(R.id.messages_threads_recycler_view);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        messagesThreadRecyclerView.setAdapter(threadedConversationRecyclerAdapter);
        messagesThreadRecyclerView.setItemViewCacheSize(500);

        threadedConversationRecyclerAdapter.addOnPagesUpdatedListener(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                if(threadedConversationRecyclerAdapter.getItemCount() < 1)
                    view.findViewById(R.id.homepage_no_message).setVisibility(View.VISIBLE);
                else
                    view.findViewById(R.id.homepage_no_message).setVisibility(View.GONE);
                return null;
            }
        });

        switch(Objects.requireNonNull(messageType)) {
            case ENCRYPTED_MESSAGES_THREAD_FRAGMENT:
                Log.d(getClass().getName(), "Fragment at encrypted");
                try {
                    threadedConversationsViewModel.getEncrypted(getContext()).observe(getViewLifecycleOwner(),
                            new Observer<PagingData<ThreadedConversations>>() {
                                @Override
                                public void onChanged(PagingData<ThreadedConversations> smsList) {
                                    threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                    view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                                }
                            });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case PLAIN_MESSAGES_THREAD_FRAGMENT:
                try {
                    threadedConversationsViewModel.getNotEncrypted(getContext()).observe(getViewLifecycleOwner(),
                            new Observer<PagingData<ThreadedConversations>>() {
                                @Override
                                public void onChanged(PagingData<ThreadedConversations> smsList) {
                                    threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                    view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                                }
                            });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case DRAFTS_MESSAGE_TYPES:
                threadedConversationsViewModel.getDrafts().observe(getViewLifecycleOwner(),
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
    }


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
//        threadedConversationsViewModel.loadNatives(getContext());

    }
}
