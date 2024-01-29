package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.paging.PagingData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.AboutActivity;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.DefaultSMS.SearchMessagesThreadsActivity;
import com.afkanerd.deku.DefaultSMS.SettingsActivity;
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.Router.Router.RouterActivity;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ThreadedConversationsFragment extends Fragment {

    ThreadedConversationsViewModel threadedConversationsViewModel;
    ThreadedConversationRecyclerAdapter threadedConversationRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    public static final String MESSAGES_THREAD_FRAGMENT_DEFAULT_MENU =
            "MESSAGES_THREAD_FRAGMENT_DEFAULT_MENU";

    public static final String MESSAGES_THREAD_FRAGMENT_DEFAULT_ACTION_MODE_MENU =
            "MESSAGES_THREAD_FRAGMENT_DEFAULT_ACTION_MODE_MENU";
    public static final String MESSAGES_THREAD_FRAGMENT_LABEL =
            "MESSAGES_THREAD_FRAGMENT_LABEL";
    public static final String MESSAGES_THREAD_FRAGMENT_NO_CONTENT =
            "MESSAGES_THREAD_FRAGMENT_NO_CONTENT";

    public static final String MESSAGES_THREAD_FRAGMENT_TYPE = "MESSAGES_THREAD_FRAGMENT_TYPE";
    public static final String ALL_MESSAGES_THREAD_FRAGMENT = "ALL_MESSAGES_THREAD_FRAGMENT";
    public static final String PLAIN_MESSAGES_THREAD_FRAGMENT = "PLAIN_MESSAGES_THREAD_FRAGMENT";
    public static final String ENCRYPTED_MESSAGES_THREAD_FRAGMENT = "ENCRYPTED_MESSAGES_THREAD_FRAGMENT";

    public static final String ARCHIVED_MESSAGE_TYPES = "ARCHIVED_MESSAGE_TYPES";
    public static final String BLOCKED_MESSAGE_TYPES = "BLOCKED_MESSAGE_TYPES";
    public static final String MUTED_MESSAGE_TYPE = "MUTED_MESSAGE_TYPE";
    public static final String DRAFTS_MESSAGE_TYPES = "DRAFTS_MESSAGE_TYPES";
    public static final String UNREAD_MESSAGE_TYPES = "UNREAD_MESSAGE_TYPES";

    public static final String AUTOMATED_MESSAGES_THREAD_FRAGMENT = "AUTOMATED_MESSAGES_THREAD_FRAGMENT";

    ActionBar actionBar;

    public interface ViewModelsInterface {
        ThreadedConversationsViewModel getThreadedConversationsViewModel();
        ExecutorService getExecutorService();
    }

    private ViewModelsInterface viewModelsInterface;

    ExecutorService executorService;

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

    protected int defaultMenu = R.menu.conversations_threads_menu;
    protected int actionModeMenu = R.menu.conversations_threads_menu_items_selected;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionBar.hide();
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(actionModeMenu, menu);

            List<String> threadsIds = new ArrayList<>();
            for(ThreadedConversationsTemplateViewHolder
                    threadedConversationsTemplateViewHolder :
                    threadedConversationRecyclerAdapter.selectedItems.getValue().values())
                threadsIds.add(threadedConversationsTemplateViewHolder.id);

            if(menu.findItem(R.id.conversations_threads_main_menu_mark_all_read) != null &&
            menu.findItem(R.id.conversations_threads_main_menu_mark_all_unread) != null)
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        boolean hasUnread = threadedConversationsViewModel.hasUnread(threadsIds);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(hasUnread) {
                                    menu.findItem(R.id.conversations_threads_main_menu_mark_all_read).setVisible(true);
                                    menu.findItem(R.id.conversations_threads_main_menu_mark_all_unread).setVisible(false);
                                }
                                else {
                                    menu.findItem(R.id.conversations_threads_main_menu_mark_all_read).setVisible(false);
                                    menu.findItem(R.id.conversations_threads_main_menu_mark_all_unread).setVisible(true);
                                }
                            }
                        });
                    }
                });
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        public Runnable getDeleteRunnable(List<String> ids) {
            return new Runnable() {
                @Override
                public void run() {

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            ThreadedConversations threadedConversations = new ThreadedConversations();
                            ThreadedConversationsDao threadedConversationsDao =
                                    threadedConversations.getDaoInstance(getContext());
                            List<String> foundList =
                                    threadedConversationsDao.findAddresses(ids);
                            threadedConversations.close();
                            threadedConversationsViewModel.delete(getContext(), ids);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                                }
                            });
                            for(String address : foundList) {
                                try {
                                    String keystoreAlias =
                                            E2EEHandler.deriveKeystoreAlias( address, 0);
                                    E2EEHandler.clear(getContext(), keystoreAlias);
                                } catch (KeyStoreException | NumberParseException |
                                         InterruptedException |
                                         NoSuchAlgorithmException | IOException |
                                         CertificateException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
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
                if(item.getItemId() == R.id.conversations_threads_main_menu_delete ||
                        item.getItemId() == R.id.archive_delete) {
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null) {
                        List<String> ids = new ArrayList<>();
                        for (ThreadedConversationsTemplateViewHolder viewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            ids.add(viewHolder.id);
                        }
                        showAlert(getDeleteRunnable(ids));
                    }
                    return true;
                }

                else if(item.getItemId() == R.id.conversations_threads_main_menu_archive) {
                    List<Archive> archiveList = new ArrayList<>();
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null)
                        for(ThreadedConversationsTemplateViewHolder templateViewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            Archive archive = new Archive();
                            archive.thread_id = templateViewHolder.id;
                            archive.is_archived = true;
                            archiveList.add(archive);
                        }
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.archive(archiveList);
                        }
                    });
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                    return true;
                }

                else if(item.getItemId() == R.id.archive_unarchive) {
                    List<Archive> archiveList = new ArrayList<>();
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null)
                        for(ThreadedConversationsTemplateViewHolder viewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            Archive archive = new Archive();
                            archive.thread_id = viewHolder.id;
                            archive.is_archived = false;
                            archiveList.add(archive);
                        }
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.unarchive(archiveList);
                        }
                    });
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                    return true;
                }

                else if(item.getItemId() == R.id.conversations_threads_main_menu_mark_all_unread) {
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null) {
                        List<String> threadIds = new ArrayList<>();
                        for (ThreadedConversationsTemplateViewHolder viewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            threadIds.add(viewHolder.id);
                        }
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                threadedConversationsViewModel.markUnRead(getContext(), threadIds);
                            }
                        });
                        threadedConversationRecyclerAdapter.resetAllSelectedItems();
                        return true;
                    }
                }

                else if(item.getItemId() == R.id.conversations_threads_main_menu_mark_all_read) {
                    if(threadedConversationRecyclerAdapter.selectedItems != null &&
                            threadedConversationRecyclerAdapter.selectedItems.getValue() != null) {
                        List<String> threadIds = new ArrayList<>();
                        for (ThreadedConversationsTemplateViewHolder viewHolder :
                                threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                            threadIds.add(viewHolder.id);
                        }
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                threadedConversationsViewModel.markRead(getContext(), threadIds);
                            }
                        });
                        threadedConversationRecyclerAdapter.resetAllSelectedItems();
                        return true;
                    }
                }
                else if(item.getItemId() == R.id.blocked_main_menu_unblock) {
                    List<String> threadIds = new ArrayList<>();
                    for (ThreadedConversationsTemplateViewHolder viewHolder :
                            threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                        threadIds.add(viewHolder.id);
                    }
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.unblock(getContext(), threadIds);
                        }
                    });
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                    return true;
                }
                else if(item.getItemId() == R.id.conversations_threads_main_menu_mute) {
                    List<String> threadIds = new ArrayList<>();
                    for (ThreadedConversationsTemplateViewHolder viewHolder :
                            threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                        threadIds.add(viewHolder.id);
                    }
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.mute(getContext(), threadIds);
                            threadedConversationsViewModel.getCount(getContext());
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    threadedConversationRecyclerAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
                    threadedConversationRecyclerAdapter.resetAllSelectedItems();
                    return true;
                }
                else if(item.getItemId() == R.id.conversation_threads_main_menu_unmute_selected) {
                    List<String> threadIds = new ArrayList<>();
                    for (ThreadedConversationsTemplateViewHolder viewHolder :
                            threadedConversationRecyclerAdapter.selectedItems.getValue().values()) {
                        threadIds.add(viewHolder.id);
                    }
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.unMute(getContext(), threadIds);
                            threadedConversationsViewModel.getCount(getContext());
                        }
                    });
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

        executorService.execute(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    ThreadedConversationsDao threadedConversationsDao;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModelsInterface = (ViewModelsInterface) view.getContext();
        executorService = viewModelsInterface.getExecutorService();

        setHasOptionsMenu(true);
        Bundle args = getArguments();

        String messageType;
        if(args != null) {
            messageType = args.getString(MESSAGES_THREAD_FRAGMENT_TYPE);
            setLabels(view, args.getString(MESSAGES_THREAD_FRAGMENT_LABEL),
                    args.getString(MESSAGES_THREAD_FRAGMENT_NO_CONTENT));
            defaultMenu = args.getInt(MESSAGES_THREAD_FRAGMENT_DEFAULT_MENU);
            actionModeMenu = args.getInt(MESSAGES_THREAD_FRAGMENT_DEFAULT_ACTION_MODE_MENU);
        } else {
            messageType = ALL_MESSAGES_THREAD_FRAGMENT;
            setLabels(view, getString(R.string.conversations_navigation_view_inbox), getString(R.string.homepage_no_message));
        }

        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);


        threadedConversationsViewModel = viewModelsInterface.getThreadedConversationsViewModel();

        threadedConversationRecyclerAdapter = new ThreadedConversationRecyclerAdapter( getContext(),
                threadedConversationsDao);
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
//        messagesThreadRecyclerView.setItemViewCacheSize(500);

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
            case UNREAD_MESSAGE_TYPES:
                threadedConversationsViewModel.getUnread().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case ARCHIVED_MESSAGE_TYPES:
                threadedConversationsViewModel.getArchived().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
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
            case BLOCKED_MESSAGE_TYPES:
                threadedConversationsViewModel.getBlocked().observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case MUTED_MESSAGE_TYPE:
                threadedConversationsViewModel.getMuted(getContext()).observe(getViewLifecycleOwner(),
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(defaultMenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.conversation_threads_main_menu_search) {
            Intent searchIntent = new Intent(getContext(), SearchMessagesThreadsActivity.class);
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(searchIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_routed) {
            Intent routingIntent = new Intent(getContext(), RouterActivity.class);
            routingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(routingIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_settings) {
            Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_about) {
            Intent aboutIntent = new Intent(getContext(), AboutActivity.class);
            aboutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(aboutIntent);
            return true;
        }
        if(item.getItemId() == R.id.conversation_threads_main_menu_clear_drafts) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        threadedConversationsViewModel.clearDrafts(getContext());
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        if(item.getItemId() == R.id.conversation_threads_main_menu_mark_all_read) {
            try {
                threadedConversationsViewModel.markAllRead(getContext());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        else if(item.getItemId() == R.id.conversation_threads_main_menu_unmute_all) {
            Contacts.unMuteAll(getContext());
            startActivity(new Intent(getContext(), ThreadedConversationsActivity.class));
            getActivity().finish();
            return true;
        }

        return false;
    }

}

