package com.example.swob_deku.Fragments.Homepage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Archive.Archive;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.example.swob_deku.Models.Messages.ViewHolders.TemplateViewHolder;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.example.swob_deku.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;

public class MessagesThreadFragment extends Fragment {
    BroadcastReceiver incomingBroadcastReceiver;
    BroadcastReceiver incomingDataBroadcastReceiver;

    MessagesThreadViewModel messagesThreadViewModel;
    MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    ArchiveHandler archiveHandler;

    Toolbar toolbar;
    ActionBar ab;

    Handler mHandler = new Handler();

    public static final String MESSAGES_THREAD_FRAGMENT_TYPE = "MESSAGES_THREAD_FRAGMENT_TYPE";
    public static final String ALL_MESSAGES_THREAD_FRAGMENT = "ALL_MESSAGES_THREAD_FRAGMENT";
    public static final String PLAIN_MESSAGES_THREAD_FRAGMENT = "PLAIN_MESSAGES_THREAD_FRAGMENT";
    public static final String ENCRYPTED_MESSAGES_THREAD_FRAGMENT = "ENCRYPTED_MESSAGES_THREAD_FRAGMENT";

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                archiveHandler = new ArchiveHandler(getContext());
            }
        }).start();

        // toolbar = view.findViewById(R.id.messages_threads_toolbar);
        toolbar = new Toolbar(getContext());
        getActivity().setActionBar(toolbar);

        messagesThreadViewModel = new ViewModelProvider(this).get(
                MessagesThreadViewModel.class);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter( getContext());

        messagesThreadRecyclerView = view.findViewById(R.id.messages_threads_recycler_view);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        try {
            messagesThreadViewModel.getMessages(getContext(), messageType).observe(getViewLifecycleOwner(),
                    new Observer<List<SMS>>() {
                        @Override
                        public void onChanged(List<SMS> smsList) {
                            TextView textView = view.findViewById(R.id.homepage_no_message);
                            if(smsList.isEmpty()) {
                                textView.setVisibility(View.VISIBLE);
                            }
                            else {
                                textView.setVisibility(View.GONE);
                            }
    //                        smsList = smsList.subList(0, 10);
                            messagesThreadRecyclerAdapter.submitList(smsList);
                        }
                    });
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }


        messagesThreadRecyclerAdapter.selectedItems.observe(getViewLifecycleOwner(),
                new Observer<HashMap<String, TemplateViewHolder>>() {
            @Override
            public void onChanged(HashMap<String, TemplateViewHolder> stringViewHolderHashMap) {
                highlightListener(stringViewHolderHashMap.size(), view);
            }
        });

        configureToolbarEvents();
        handleIncomingMessage();
        enableSwipeAction();
        setRefreshTimer();
    }

    private void configureToolbarEvents() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String[] ids = messagesThreadRecyclerAdapter.selectedItems.getValue()
                        .keySet().toArray(new String[0]);
                if(item.getItemId() == R.id.threads_delete) {
                    try {
                        SMSHandler.deleteThreads(getContext(), ids);
                        messagesThreadRecyclerAdapter.resetAllSelectedItems();
                        messagesThreadViewModel.informChanges(getContext());
                        return true;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                else if(item.getItemId() == R.id.threads_archive) {
                    long[] longArr = new long[ids.length];
                    for (int i = 0; i < ids.length; i++)
                        longArr[i] = Long.parseLong(ids[i]);

                    try {
                        archiveHandler.archiveMultipleSMS(getContext(), longArr);
                        messagesThreadRecyclerAdapter.resetAllSelectedItems();
                        messagesThreadViewModel.informChanges(getContext());
                        return true;
                    } catch (InterruptedException | GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    private void highlightListener(int size, View view){
        Menu menu = toolbar.getMenu();
        if(size < 1) {
            menu.setGroupVisible(R.id.threads_menu, false);
            view.findViewById(R.id.messages_thread_search_input_constrain).setVisibility(View.VISIBLE);
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setHomeAsUpIndicator(null);
        } else {
            view.findViewById(R.id.messages_thread_search_input_constrain).setVisibility(View.GONE);
            menu.setGroupVisible(R.id.threads_menu, true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
            ab.setTitle(String.valueOf(size));
        }
    }

    private void setRefreshTimer() {
        final int recyclerViewTimeUpdateLimit = 60 * 1000;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(messagesThreadRecyclerAdapter.selectedItems.getValue()==null ||
                        messagesThreadRecyclerAdapter.selectedItems.getValue().isEmpty())
                    messagesThreadRecyclerAdapter.notifyDataSetChanged();
                mHandler.postDelayed(this, recyclerViewTimeUpdateLimit);
            }
        }, recyclerViewTimeUpdateLimit);
    }
    private void enableSwipeAction() {
        final RecyclerView.ViewHolder[] currentViewHolder = {null};
//        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

//        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.RIGHT) {
        ItemTouchHelper.SimpleCallback swipeArchiveCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.RIGHT ) {
            private Drawable deleteIcon;
            private int intrinsicWidth;
            private int intrinsicHeight;

            private int mSwipeSlop;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                /**
                 * TODO: swip RIGHT, - archive
                 * TODO: swip LEFT - delete
                 * TODO: increase threshold before permanent delete
                 */
                TemplateViewHolder itemView = (TemplateViewHolder) viewHolder;
                String threadId = itemView.id;
                try {
                    Archive archive = new Archive(Long.parseLong(threadId));
                    archiveHandler.archiveSMS(getContext(), archive);
                    messagesThreadViewModel.informChanges(getContext());
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true; // Enable long-press drag functionality
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if(viewHolder != null)
                    currentViewHolder[0] = viewHolder;

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.archive_slide_drawable);
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.messages_default_drawable);
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (deleteIcon == null) {
                        deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.round_archive_24);
                        intrinsicWidth = deleteIcon.getIntrinsicWidth();
                        intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    }

                    float iconMargin = (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconBottom = iconTop + intrinsicHeight;
                    float iconLeft, iconRight;

                    if (mSwipeSlop == 0) {
                        mSwipeSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
                    }
                    float threshold = mSwipeSlop * 3;

                    // Set swipe distance limit
                    if (Math.abs(dX) > threshold) {
                        dX = Math.signum(dX) * threshold;
                    }

                    if (dX > 0) {
                        iconLeft = viewHolder.itemView.getLeft() + iconMargin;
                        iconRight = viewHolder.itemView.getLeft() + iconMargin + intrinsicWidth;
                    } else {
                        iconRight = viewHolder.itemView.getRight() - iconMargin;
                        iconLeft = viewHolder.itemView.getRight() - iconMargin - intrinsicWidth;
                    }

                    deleteIcon.setBounds((int) iconLeft, (int) iconTop, (int) iconRight, (int) iconBottom);

                    View itemView = viewHolder.itemView;
                    int itemHeight = itemView.getHeight();
                    int itemWidth = itemView.getWidth();
                    int itemLeft = itemView.getLeft();
                    int itemRight = itemView.getRight();

                    Paint p = new Paint();
                    p.setColor(getContext().getColor(R.color.light_blue));

                    c.drawRect(itemLeft, itemView.getTop(), itemRight, itemView.getBottom(), p);
                    deleteIcon.draw(c);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                currentViewHolder[0] = null;
            }
        };

        ItemTouchHelper.SimpleCallback swipeDeleteCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT ) {
            private Drawable deleteIcon;
            private int intrinsicWidth;
            private int intrinsicHeight;

            private int mSwipeSlop;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                /**
                 * TODO: swip RIGHT, - archive
                 * TODO: swip LEFT - delete
                 * TODO: increase threshold before permanent delete
                 */
                TemplateViewHolder itemView = (TemplateViewHolder) viewHolder;
                String threadId = itemView.id;
                try {
                    Cursor cursor = SMSHandler.fetchSMSForThread(getContext(), threadId, 1, 0);
                    if(cursor.moveToFirst()) {
                        SecurityECDH securityECDH = new SecurityECDH(getContext());
                        String address = new SMS(cursor).getAddress();
                        securityECDH.removeAllKeys(Helpers.formatPhoneNumbers(getContext(), address));
                        SMSHandler.deleteThread(getContext(), threadId);
                    }
                    cursor.close();
                    messagesThreadViewModel.informChanges(getContext());
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true; // Enable long-press drag functionality
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if(viewHolder != null)
                    currentViewHolder[0] = viewHolder;

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.received_messages_drawable);
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.messages_default_drawable);
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (deleteIcon == null) {
                        deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.round_delete_24);
                        intrinsicWidth = deleteIcon.getIntrinsicWidth();
                        intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    }

                    float iconMargin = (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconBottom = iconTop + intrinsicHeight;
                    float iconLeft, iconRight;

                    if (mSwipeSlop == 0) {
                        mSwipeSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
                    }
                    float threshold = mSwipeSlop * 3;

                    // Set swipe distance limit
                    if (Math.abs(dX) > threshold) {
                        dX = Math.signum(dX) * threshold;
                    }

                    if (dX > 0) {
                        iconLeft = viewHolder.itemView.getLeft() + iconMargin;
                        iconRight = viewHolder.itemView.getLeft() + iconMargin + intrinsicWidth;
                    } else {
                        iconRight = viewHolder.itemView.getRight() - iconMargin;
                        iconLeft = viewHolder.itemView.getRight() - iconMargin - intrinsicWidth;
                    }

                    deleteIcon.setBounds((int) iconLeft, (int) iconTop, (int) iconRight, (int) iconBottom);

                    View itemView = viewHolder.itemView;
                    int itemHeight = itemView.getHeight();
                    int itemWidth = itemView.getWidth();
                    int itemLeft = itemView.getLeft();
                    int itemRight = itemView.getRight();

                    Paint p = new Paint();
                    p.setColor(getContext().getColor(R.color.delete_red));

                    c.drawRect(itemLeft, itemView.getTop(), itemRight, itemView.getBottom(), p);
                    deleteIcon.draw(c);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                currentViewHolder[0] = null;
            }
        };

        ItemTouchHelper itemTouchHelperArchive = new ItemTouchHelper(swipeArchiveCallback);
        ItemTouchHelper itemTouchHelperDelete = new ItemTouchHelper(swipeDeleteCallback);

        itemTouchHelperArchive.attachToRecyclerView(messagesThreadRecyclerView);
        itemTouchHelperDelete.attachToRecyclerView(messagesThreadRecyclerView);
    }

    private void handleIncomingMessage() {
        incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    messagesThreadViewModel.informChanges(getContext());
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }
        };

        incomingDataBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    messagesThreadViewModel.informChanges(getContext());
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        getContext().registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        getContext().registerReceiver(incomingDataBroadcastReceiver,
                new IntentFilter(IncomingDataSMSBroadcastReceiver.DATA_BROADCAST_INTENT));

        getContext().registerReceiver(incomingBroadcastReceiver,
                new IntentFilter(SMSHandler.MESSAGE_STATE_CHANGED_BROADCAST_INTENT));
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            messagesThreadViewModel.informChanges(getContext());
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && messagesThreadRecyclerAdapter.selectedItems.getValue() != null) {
            messagesThreadRecyclerAdapter.resetAllSelectedItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if(incomingBroadcastReceiver != null)
            getContext().unregisterReceiver(incomingBroadcastReceiver);
        super.onDestroy();
    }
}
