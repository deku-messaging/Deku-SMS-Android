package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ThreadedConversationsFragment extends Fragment {

    ThreadedConversationsViewModel threadedConversationsViewModel;
    ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    ThreadedConversations threadedConversations = new ThreadedConversations();

    public static final String MESSAGES_THREAD_FRAGMENT_TYPE = "MESSAGES_THREAD_FRAGMENT_TYPE";
    public static final String ALL_MESSAGES_THREAD_FRAGMENT = "ALL_MESSAGES_THREAD_FRAGMENT";
    public static final String PLAIN_MESSAGES_THREAD_FRAGMENT = "PLAIN_MESSAGES_THREAD_FRAGMENT";
    public static final String ENCRYPTED_MESSAGES_THREAD_FRAGMENT = "ENCRYPTED_MESSAGES_THREAD_FRAGMENT";

    public static final String DRAFTS_MESSAGE_TYPES = "DRAFTS_MESSAGE_TYPES";

    public static final String AUTOMATED_MESSAGES_THREAD_FRAGMENT = "AUTOMATED_MESSAGES_THREAD_FRAGMENT";

    ActionBar actionBar;

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

    ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//            Objects.requireNonNull(requireActivity().getActionBar()).hide();
            actionBar.hide();
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.conversations_threads_menu_items_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        public Runnable getDeleteRunnable(List<String> ids,
                                          List<ThreadedConversations> threadedConversationsList) {
            return new Runnable() {
                @Override
                public void run() {
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ThreadedConversations threadedConversations = new ThreadedConversations();
                            ThreadedConversationsDao threadedConversationsDao =
                                    threadedConversations.getDaoInstance(getContext());
                            List<ThreadedConversations> foundList =
                                    threadedConversationsDao.find(ids);
                            for(ThreadedConversations threadedConversation :
                                    foundList) {
                                try {
                                    String keystoreAlias =
                                            E2EEHandler.deriveKeystoreAlias(
                                                    threadedConversation.getAddress(),
                                                    0);
                                    E2EEHandler.removeFromKeystore(getContext(), keystoreAlias);
                                    E2EEHandler.removeFromEncryptionDatabase(getContext(),
                                            keystoreAlias);
                                } catch (KeyStoreException | NumberParseException |
                                         InterruptedException |
                                         NoSuchAlgorithmException | IOException |
                                         CertificateException e) {
                                    e.printStackTrace();
                                }
                            }
                            threadedConversationsViewModel.delete(getContext(),
                                    threadedConversationsList);
                        }
                    }).start();
                }
            };
        }
        private void showAlert(Runnable runnable) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(threadedConversationRecyclerAdapter != null) {
                if(item.getItemId() == R.id.conversations_threads_main_menu_delete) {
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null) {
                        List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
                        List<String> ids = new ArrayList<>();
                        for (ThreadedConversationsTemplateViewHolder viewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            ThreadedConversations threadedConversation = new ThreadedConversations();
                            threadedConversation.setThread_id(viewHolder.id);
                            threadedConversationsList.add(threadedConversation);
                            ids.add(threadedConversation.getThread_id());
                        }
                        showAlert(getDeleteRunnable(ids, threadedConversationsList));
                    }
                    return true;
                }

                if(item.getItemId() == R.id.conversations_threads_main_menu_archive) {
                    List<Archive> archiveList = new ArrayList<>();
                    for(ThreadedConversationsTemplateViewHolder templateViewHolder :
                            threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                        Archive archive = new Archive();
                        archive.thread_id = templateViewHolder.id;
                        archive.is_archived = true;
                        archiveList.add(archive);
                    }
                    threadedConversationsViewModel.archive(archiveList);
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                    return true;
                }

            }
            return false;
        }

        // Called when the user exits the action mode.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionBar.show();
            actionMode = null;
            if(threadedConversationRecyclerAdapter != null)
                threadedConversationRecyclerAdapter.resetAllSelectedItems();
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        if(getContext() != null) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            if(sharedPreferences.getBoolean(getString(R.string.configs_load_natives), true)) {
                sharedPreferences.edit().putBoolean(getString(R.string.configs_load_natives), false)
                        .apply();
                threadedConversationsViewModel.reset(getContext());
            }

            threadedConversationsViewModel.refresh(getContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String messageType = args == null ? ALL_MESSAGES_THREAD_FRAGMENT :
                args.getString(MESSAGES_THREAD_FRAGMENT_TYPE);

        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);

        setLabels(view, getString(R.string.conversations_navigation_view_inbox), getString(R.string.homepage_no_message));

        threadedConversationsViewModel = new ViewModelProvider(this).get(
                ThreadedConversationsViewModel.class);
        threadedConversationsViewModel.threadedConversationsDao =
                threadedConversations.getDaoInstance(getContext());
        threadedConversationRecyclerAdapter = new ThreadedConversationRecyclerAdapter( getContext(),
                threadedConversationsViewModel.threadedConversationsDao);
        threadedConversationRecyclerAdapter.selectedItems.observe(getViewLifecycleOwner(),
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
                } else if(actionMode == null) {
                    actionMode = getActivity().startActionMode(actionModeCallback);
                }
                if(actionMode != null)
                    actionMode.setTitle(
                            String.valueOf(threadedConversationsTemplateViewHolders.size()));
            }
        });

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
}
