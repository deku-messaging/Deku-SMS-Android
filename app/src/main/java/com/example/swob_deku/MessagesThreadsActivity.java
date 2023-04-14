package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Archive.Archive;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityDH;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.List;

public class MessagesThreadsActivity extends AppCompatActivity {
    // TODO: Change address to friendly name if in phonebook
    MessagesThreadViewModel messagesThreadViewModel;
    MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;
    RecyclerView messagesThreadRecyclerView;

    BroadcastReceiver incomingBroadcastReceiver;

    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_threads);

        if(!checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
            return;
        }

//        cancelAllNotifications();
        handleIncomingMessage();

        messagesThreadViewModel = new ViewModelProvider(this).get(
                MessagesThreadViewModel.class);

        TextInputEditText searchTextView = findViewById(R.id.recent_search_edittext_clickable);
        searchTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    startActivity(new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class));
                }
            }
        });
        searchTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class));
            }
        });

        TextInputLayout searchTextViewLayout = findViewById(R.id.search_messages_text_clickable);
        searchTextViewLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.messages_threads_menu_item_settings) {
                            Intent settingsIntent = new Intent(getApplicationContext(),
                                    GatewayServerListingActivity.class);
                            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(settingsIntent);
                            return true;
                        }
                        else if (item.getItemId() == R.id.messages_threads_menu_item_archived) {
                            Intent archivedIntent = new Intent(getApplicationContext(),
                                    ArchivedMessagesActivity.class);
                            archivedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(archivedIntent);
                            return true;
                        }
                        return false;
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.main_menu, popup.getMenu());
                popup.show();
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout);

        messagesThreadRecyclerView = findViewById(R.id.messages_threads_recycler_view);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        messagesThreadViewModel.getMessages(getApplicationContext()).observe(this,
                new Observer<List<SMS>>() {
                    @Override
                    public void onChanged(List<SMS> smsList) {
                        messagesThreadRecyclerAdapter.submitList(smsList);
                    }
                });

        enableSwipeAction();
        Log.d(getLocalClassName(), "Threading main activity");
//        try {
//            Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
//            KeyPairGenerator aliceKpg = KeyPairGenerator.getInstance("ECDH", "SC");
//            aliceKpg.initialize(256);
//            KeyPair aliceKp = aliceKpg.generateKeyPair();
//        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
//            throw new RuntimeException(e);
//        }

        setRefreshTimer();

    }

    private boolean checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
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
                MessagesThreadRecyclerAdapter.ViewHolder itemView = (MessagesThreadRecyclerAdapter.ViewHolder) viewHolder;
                String threadId = itemView.id;
                try {
                    Archive archive = new Archive(Long.parseLong(threadId));
                    ArchiveHandler.archiveSMS(getApplicationContext(), archive);
                    messagesThreadViewModel.informChanges(getApplicationContext());
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
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.sent_messages_drawable);
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

                // Change background color of swiped item
//                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//                    // Calculate the left and right positions of the swiped item
//                    View itemView = viewHolder.itemView;
//                    int itemHeight = itemView.getHeight();
//                    int itemWidth = itemView.getWidth();
//                    int itemLeft = itemView.getLeft();
//                    int itemRight = itemView.getRight();
//
//                    Paint p = new Paint();
//                    p.setColor(getColor(R.color.text_box));
//
//                    c.drawRect(itemLeft, itemView.getTop(), itemRight, itemView.getBottom(), p);
//                }
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (deleteIcon == null) {
                        deleteIcon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_archive_24);
                        intrinsicWidth = deleteIcon.getIntrinsicWidth();
                        intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    }

                    float iconMargin = (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconBottom = iconTop + intrinsicHeight;
                    float iconLeft, iconRight;

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
                    p.setColor(getColor(R.color.default_gray));

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
                MessagesThreadRecyclerAdapter.ViewHolder itemView = (MessagesThreadRecyclerAdapter.ViewHolder) viewHolder;
                String threadId = itemView.id;
                try {
                    Cursor cursor = SMSHandler.fetchSMSForThread(getApplicationContext(), threadId, 1, 0);
                    if(cursor.moveToFirst()) {
                        SecurityDH securityDH = new SecurityDH(getApplicationContext());
                        String address = new SMS(cursor).getAddress();
                        securityDH.removeAllKeys(Helpers.formatPhoneNumbers(address));
                        SMSHandler.deleteThread(getApplicationContext(), threadId);
                    }
                    cursor.close();
                    messagesThreadViewModel.informChanges(getApplicationContext());
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
                    currentViewHolder[0].itemView.setBackgroundResource(R.drawable.sent_messages_drawable);
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

                // Change background color of swiped item
//                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//                    // Calculate the left and right positions of the swiped item
//                    View itemView = viewHolder.itemView;
//                    int itemHeight = itemView.getHeight();
//                    int itemWidth = itemView.getWidth();
//                    int itemLeft = itemView.getLeft();
//                    int itemRight = itemView.getRight();
//
//                    Paint p = new Paint();
//                    p.setColor(getColor(R.color.text_box));
//
//                    c.drawRect(itemLeft, itemView.getTop(), itemRight, itemView.getBottom(), p);
//                }
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (deleteIcon == null) {
                        deleteIcon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_delete_24);
                        intrinsicWidth = deleteIcon.getIntrinsicWidth();
                        intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    }

                    float iconMargin = (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - intrinsicHeight) / 2.0f;
                    float iconBottom = iconTop + intrinsicHeight;
                    float iconLeft, iconRight;

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
                    p.setColor(getColor(R.color.delete_red));

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

    private void setRefreshTimer() {
        final int recyclerViewTimeUpdateLimit = 60 * 1000;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                messagesThreadRecyclerAdapter.notifyDataSetChanged();
                mHandler.postDelayed(this, recyclerViewTimeUpdateLimit);
            }
        }, recyclerViewTimeUpdateLimit);
    }
    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancelAll();
    }

    public void onNewMessageClick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    public void onRouterClick(View view) {
        startActivity(new Intent(this, RouterActivity.class));
    }

    private void handleIncomingMessage() {
        incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                messagesThreadViewModel.informChanges(getApplicationContext());
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        registerReceiver(incomingBroadcastReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (1) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor contactCursor = getApplicationContext().getContentResolver().query(
                            contactData,
                            null,
                            null,
                            null,
                            null);

                    if(contactCursor != null) {
                        if (contactCursor.moveToFirst()) {
                            int contactIndexInformation = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String number = contactCursor.getString(contactIndexInformation);

                            Intent singleMessageThreadIntent = new Intent(this, SMSSendActivity.class);
                            singleMessageThreadIntent.putExtra(SMSSendActivity.ADDRESS, number);
                            startActivity(singleMessageThreadIntent);
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.messages_threads_recycler_view).requestFocus();
        messagesThreadViewModel.informChanges(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        if(incomingBroadcastReceiver != null)
            unregisterReceiver(incomingBroadcastReceiver);
        super.onDestroy();
    }
}