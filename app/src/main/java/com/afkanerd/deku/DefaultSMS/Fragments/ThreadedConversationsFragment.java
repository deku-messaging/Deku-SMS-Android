package com.afkanerd.deku.DefaultSMS.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.telecom.TelecomManager;
import android.util.TypedValue;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.paging.PagingData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.AboutActivity;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.DefaultSMS.SearchMessagesThreadsActivity;
import com.afkanerd.deku.DefaultSMS.SettingsActivity;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerRoutedActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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

    ActionMode actionMode;
    public Runnable getDeleteRunnable(List<String> ids) {
        return new Runnable() {
            @Override
            public void run() {

                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        threadedConversationsViewModel.delete(getContext(), ids);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                threadedConversationRecyclerAdapter.resetAllSelectedItems();
                            }
                        });
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
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                        ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                        ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.mute(threadIds);
                            threadedConversationsViewModel.getCount();
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
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            threadedConversationsViewModel.unMute(threadIds);
                            threadedConversationsViewModel.getCount();
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

        ThreadingPoolExecutor.executorService.execute(new Runnable() {
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
                }
            }
        });
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewModelsInterface viewModelsInterface = (ViewModelsInterface) view.getContext();

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

        threadedConversationRecyclerAdapter = new ThreadedConversationRecyclerAdapter();
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
                try {
                    threadedConversationsViewModel.getEncrypted()
                            .observe(getViewLifecycleOwner(),
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
                threadedConversationsViewModel.getUnread(requireContext())
                        .observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case ARCHIVED_MESSAGE_TYPES:
                threadedConversationsViewModel.getArchived(requireContext())
                        .observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case DRAFTS_MESSAGE_TYPES:
                threadedConversationsViewModel.getDrafts(requireContext())
                        .observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case BLOCKED_MESSAGE_TYPES:
                threadedConversationsViewModel.getBlocked(requireContext())
                        .observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
                break;
            case MUTED_MESSAGE_TYPE:
                threadedConversationsViewModel.getMuted(requireContext())
                        .observe(getViewLifecycleOwner(),
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
                threadedConversationsViewModel.get(requireContext()).observe(getViewLifecycleOwner(),
                        new Observer<PagingData<ThreadedConversations>>() {
                            @Override
                            public void onChanged(PagingData<ThreadedConversations> smsList) {
                                threadedConversationRecyclerAdapter.submitData(getLifecycle(), smsList);
                                view.findViewById(R.id.homepage_messages_loader).setVisibility(View.GONE);
                            }
                        });
        }
        swipeActions();
    }

    private void swipeActions() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper
                .SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if(direction == ItemTouchHelper.LEFT) {
                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            ThreadedConversations threadedConversations =
                                    threadedConversationRecyclerAdapter
                                            .getItemByPosition(viewHolder.getLayoutPosition());
                            threadedConversationsViewModel
                                    .archive(threadedConversations.getThread_id());
                        }
                    });
                } else if(direction == ItemTouchHelper.RIGHT) {
                    ThreadedConversations threadedConversations =
                            threadedConversationRecyclerAdapter
                                    .getItemByPosition(viewHolder.getLayoutPosition());
                    showAlert(getDeleteRunnable(new ArrayList<>(
                            Collections.singletonList(threadedConversations.getThread_id()))));
                    threadedConversationRecyclerAdapter.
                            notifyItemChanged(viewHolder.getLayoutPosition());
                }
//                threadedConversationRecyclerAdapter.notifyItemRemoved(viewHolder.getLayoutPosition());
            }
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                boolean isCancelled = dX == 0 && !isCurrentlyActive;

                if (isCancelled) {
                    clearCanvas(c, viewHolder.itemView.getRight() + dX,
                            (float) viewHolder.itemView.getTop(),
                            (float) viewHolder.itemView.getRight(),
                            (float) viewHolder.itemView.getBottom());
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState,
                            isCurrentlyActive);
                    return;
                }

                int xMarkMargin;
                xMarkMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                        getResources().getDisplayMetrics());

                int drawable = dX > 0 ? R.drawable.round_delete_24 : R.drawable.round_archive_24;
                Drawable deleteIcon = ContextCompat.getDrawable(getContext(), drawable);
                int itemHeight = viewHolder.itemView.getBottom() - viewHolder.itemView.getTop();
                int intrinsicWidth = deleteIcon.getIntrinsicWidth();
                int intrinsicHeight = deleteIcon.getIntrinsicHeight();

                ColorDrawable background = new ColorDrawable(getContext()
                        .getColor(R.color.md_theme_background));
                int xMarkRight = 0, xMarkLeft=0;
                if(dX > 0) {
                    // Delete
                    xMarkRight = viewHolder.itemView.getLeft() + xMarkMargin + intrinsicWidth;
                    xMarkLeft = xMarkRight - intrinsicWidth;
                    background = new ColorDrawable(getContext()
                            .getColor(R.color.md_theme_tertiary));
                } else if(dX < 0) {
                    // Archive
                    xMarkLeft = viewHolder.itemView.getRight() - xMarkMargin - intrinsicWidth;
                    xMarkRight = xMarkLeft + intrinsicWidth;
                }
                background.setBounds(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop(),
                        viewHolder.itemView.getRight(), viewHolder.itemView.getBottom());
                background.draw(c);

                int xMarkTop = viewHolder.itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int xMarkBottom = xMarkTop + intrinsicHeight;

                deleteIcon.setBounds(xMarkLeft, xMarkTop + 16, xMarkRight, xMarkBottom);
                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
                Paint mClearPaint = new Paint();
                mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                c.drawRect(left, top, right, bottom, mClearPaint);

            }
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.7f;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(messagesThreadRecyclerView);
    }

    private static final int CREATE_FILE = 777;
    public void exportInbox() {
        // Request code for creating a PDF document.

        String filename = "deku_sms_backup_" + System.currentTimeMillis() + ".json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (resultData != null) {
                Uri uri = resultData.getData();
                // Perform operations on the document using its URI.

                if(uri == null)
                    return;

                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ParcelFileDescriptor pfd = requireActivity().getContentResolver().
                                    openFileDescriptor(uri, "w");
                            FileOutputStream fileOutputStream =
                                    new FileOutputStream(pfd.getFileDescriptor());
                            fileOutputStream.write(threadedConversationsViewModel.getAllExport()
                                    .getBytes());
                            // Let the document provider know you're done by closing the stream.
                            fileOutputStream.close();
                            pfd.close();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(),
                                            getString(R.string.conversations_exported_complete),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
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
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_refresh) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    threadedConversationsViewModel.reset(getContext());
                }
            });
            return true;
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_routed) {
            Intent routingIntent = new Intent(getContext(), GatewayServerRoutedActivity.class);
            routingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(routingIntent);
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_settings) {
            Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
            startActivity(settingsIntent);
        }
        if (item.getItemId() == R.id.conversation_threads_main_menu_about) {
            Intent aboutIntent = new Intent(getContext(), AboutActivity.class);
            aboutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(aboutIntent);
        }
        if(item.getItemId() == R.id.conversation_threads_main_menu_clear_drafts) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        threadedConversationsViewModel.clearDrafts(getContext());
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if(item.getItemId() == R.id.conversation_threads_main_menu_mark_all_read) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    threadedConversationsViewModel.markAllRead(getContext());
                }
            });
        }
        else if(item.getItemId() == R.id.conversation_threads_main_menu_unmute_all) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    threadedConversationsViewModel.unMuteAll();
                }
            });
        }
        else if(item.getItemId() == R.id.conversations_menu_export) {
            exportInbox();
        }
        else if(item.getItemId() == R.id.blocked_main_menu_unblock_manager_id) {
            TelecomManager telecomManager = (TelecomManager)
                    getContext().getSystemService(Context.TELECOM_SERVICE);
            startActivity(telecomManager.createManageBlockedNumbersIntent(), null);
            return true;
        }
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                threadedConversationsViewModel.getCount();
            }
        });

        return true;
    }

}

