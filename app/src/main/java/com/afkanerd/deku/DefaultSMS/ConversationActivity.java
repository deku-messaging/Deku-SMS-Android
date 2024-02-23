package com.afkanerd.deku.DefaultSMS;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsHandler;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationTemplateViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.E2EECompactActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ConversationActivity extends E2EECompactActivity {
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String SEARCH_STRING = "SEARCH_STRING";
    public static final String SEARCH_INDEX = "SEARCH_INDEX";
    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    ActionMode actionMode;
    ConversationsRecyclerAdapter conversationsRecyclerAdapter;
    TextInputEditText smsTextView;
    MutableLiveData<String> mutableLiveDataComposeMessage = new MutableLiveData<>();

    LinearLayoutManager linearLayoutManager;
    RecyclerView singleMessagesThreadRecyclerView;

    MutableLiveData<List<Integer>> searchPositions = new MutableLiveData<>();

    ImageButton backSearchBtn;
    ImageButton forwardSearchBtn;

    Toolbar toolbar;

    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
//        test();

        toolbar = (Toolbar) findViewById(R.id.conversation_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            configureActivityDependencies();
            instantiateGlobals();
            configureToolbars();
            configureRecyclerView();
            configureMessagesTextBox();

            configureLayoutForMessageType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy.
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance.
        smsTextView.setText(savedInstanceState.getString(DRAFT_TEXT));

        savedInstanceState.remove(DRAFT_TEXT);
        savedInstanceState.remove(DRAFT_ID);
    }

    private void test() {
        if(BuildConfig.DEBUG)
            getIntent().putExtra(SEARCH_STRING, "Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
        layout.requestFocus();

        if(threadedConversations.is_secured)
            layout.setPlaceholderText(getString(R.string.send_message_secured_text_box_hint));

        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    NativeSMSDB.Incoming.update_read(getApplicationContext(), 1,
                            threadedConversations.getThread_id(), null);
                    conversationsViewModel.updateToRead(getApplicationContext());
                    threadedConversations.setIs_read(true);
                    databaseConnector.threadedConversationsDao().update(threadedConversations);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.conversations_menu, menu);
            if (isShortCode) {
                menu.findItem(R.id.conversation_main_menu_call).setVisible(false);
                menu.findItem(R.id.conversation_main_menu_encrypt_lock).setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(Contacts.isMuted(getApplicationContext(), threadedConversations.getAddress())) {
            menu.findItem(R.id.conversations_menu_unmute).setVisible(true);
            menu.findItem(R.id.conversations_menu_mute).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void resetSearch() {
        findViewById(R.id.conversations_search_results_found).setVisibility(View.GONE);
        conversationsRecyclerAdapter.searchString = null;
        conversationsRecyclerAdapter.resetSearchItems(searchPositions.getValue());
        searchPositions = new MutableLiveData<>();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.conversation_main_menu_call == item.getItemId()) {
            ThreadedConversationsHandler.call(getApplicationContext(), threadedConversations);
            return true;
        }
        else if(R.id.conversation_main_menu_search == item.getItemId()) {
            Intent intent = new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class);
            intent.putExtra(Conversation.THREAD_ID, threadedConversations.getThread_id());
            startActivity(intent);
        }
        else if (R.id.conversations_menu_block == item.getItemId()) {
            blockContact();
            if(actionMode != null)
                actionMode.finish();
            return true;
        }
        else if (R.id.conversations_menu_mute == item.getItemId()) {
            Contacts.mute(getApplicationContext(), threadedConversations.getAddress());
            invalidateMenu();
            configureToolbars();
            Toast.makeText(getApplicationContext(), getString(R.string.conversation_menu_muted),
                    Toast.LENGTH_SHORT).show();
            if(actionMode != null)
                actionMode.finish();
            return true;
        }
        else if (R.id.conversations_menu_unmute == item.getItemId()) {
            Contacts.unmute(getApplicationContext(), threadedConversations.getAddress());
            invalidateMenu();
            configureToolbars();
            Toast.makeText(getApplicationContext(), getString(R.string.conversation_menu_unmuted),
                    Toast.LENGTH_SHORT).show();
            if(actionMode != null)
                actionMode.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureActivityDependencies() throws Exception {
        /**
         * Address = This could come from Shared Intent, Contacts etc
         * ThreadID = This comes from Thread screen and notifications
         * ThreadID is the intended way of populating the messages
         * ==> If not ThreadId do not populate, everything else should take the pleasure of finding
         * and sending a threadID to this intent
         */
        if(getIntent().getAction() != null && (getIntent().getAction().equals(Intent.ACTION_SENDTO) ||
                getIntent().getAction().equals(Intent.ACTION_SEND))) {
            String sendToString = getIntent().getDataString();
            if (sendToString != null && (sendToString.contains("smsto:") ||
                    sendToString.contains("sms:"))) {
                String defaultRegion = Helpers.getUserCountry(getApplicationContext());
                String address = Helpers.getFormatCompleteNumber(sendToString, defaultRegion);
                getIntent().putExtra(Conversation.ADDRESS, address);
            }
        }

        if(!getIntent().hasExtra(Conversation.THREAD_ID) &&
                !getIntent().hasExtra(Conversation.ADDRESS)) {
            throw new Exception("No threadId nor Address supplied for activity");
        }
        if(getIntent().hasExtra(Conversation.THREAD_ID)) {
            ThreadedConversations threadedConversations = new ThreadedConversations();
            threadedConversations.setThread_id(getIntent().getStringExtra(Conversation.THREAD_ID));
            this.threadedConversations = ThreadedConversationsHandler.get(
                    databaseConnector.threadedConversationsDao(), threadedConversations);
        }
        else if(getIntent().hasExtra(Conversation.ADDRESS)) {
            ThreadedConversations threadedConversations = new ThreadedConversations();
            threadedConversations.setAddress(getIntent().getStringExtra(Conversation.ADDRESS));
            this.threadedConversations = ThreadedConversationsHandler.get(getApplicationContext(),
                    getIntent().getStringExtra(Conversation.ADDRESS));
        }

        final String defaultUserCountry = Helpers.getUserCountry(getApplicationContext());
        final String address = this.threadedConversations.getAddress();
        this.threadedConversations.setAddress(
                Helpers.getFormatCompleteNumber(address, defaultUserCountry));
        String contactName = Contacts.retrieveContactName(getApplicationContext(),
                this.threadedConversations.getAddress());
        if(contactName == null) {
            this.threadedConversations.setContact_name(Helpers.getFormatNationalNumber(address,
                    defaultUserCountry ));
        } else {
            this.threadedConversations.setContact_name(contactName);
        }

        setEncryptionThreadedConversations(this.threadedConversations);
        isShortCode = Helpers.isShortCode(this.threadedConversations);
    }

    int searchPointerPosition;
    TextView searchFoundTextView;

    private void scrollRecyclerViewSearch(int position) {
        if(position == -2){
            String text = "0/0 " + getString(R.string.conversations_search_results_found);
            searchFoundTextView.setText(text);
            return;
        }

        conversationsRecyclerAdapter.refresh();
        if(position != -3)
            singleMessagesThreadRecyclerView.scrollToPosition(position);
        String text = (searchPointerPosition == -1 ?
                0 :
                searchPointerPosition + 1) + "/" + searchPositions.getValue().size() + " " + getString(R.string.conversations_search_results_found);
        searchFoundTextView.setText(text);
    }

    private void instantiateGlobals() throws GeneralSecurityException, IOException {
        searchFoundTextView = findViewById(R.id.conversations_search_results_found_counter_text);

        backSearchBtn = findViewById(R.id.conversation_search_found_back_btn);
        forwardSearchBtn = findViewById(R.id.conversation_search_found_forward_btn);

        smsTextView = findViewById(R.id.conversation_send_text_input);
        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(false);
        linearLayoutManager.setReverseLayout(true);
        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);

        conversationsRecyclerAdapter = new ConversationsRecyclerAdapter(threadedConversations);

        conversationsViewModel = new ViewModelProvider(this)
                .get(ConversationsViewModel.class);
        conversationsViewModel.datastore = Datastore.datastore;

        backSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchPointerPosition <= 0)
                    searchPointerPosition = searchPositions.getValue().size();
                scrollRecyclerViewSearch(searchPositions.getValue().get(--searchPointerPosition));
            }
        });

        forwardSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (searchPointerPosition >= searchPositions.getValue().size() -1)
                    searchPointerPosition = -1;
                scrollRecyclerViewSearch(searchPositions.getValue().get(++searchPointerPosition));
            }
        });

        searchPositions.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> integers) {
                if(!integers.isEmpty()) {
                    searchPointerPosition = 0;
                    scrollRecyclerViewSearch(
                            firstScrollInitiated ?
                            searchPositions.getValue().get(searchPointerPosition):-3);
                } else {
                    conversationsRecyclerAdapter.searchString = null;
                    scrollRecyclerViewSearch(-2);
                }
            }
        });

    }

    boolean firstScrollInitiated = false;

    LifecycleOwner lifecycleOwner;

    Conversation conversation = new Conversation();
    private void configureRecyclerView() throws InterruptedException {
        singleMessagesThreadRecyclerView.setAdapter(conversationsRecyclerAdapter);
        singleMessagesThreadRecyclerView.setItemViewCacheSize(500);

        lifecycleOwner = this;

        conversationsRecyclerAdapter.addOnPagesUpdatedListener(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                if(conversationsRecyclerAdapter.getItemCount() < 1 &&
                        getIntent().getAction() != null &&
                        !getIntent().getAction().equals(Intent.ACTION_SENDTO) &&
                        !getIntent().getAction().equals(Intent.ACTION_SEND))
                    finish();
                else if(searchPositions != null && searchPositions.getValue() != null
                        && !searchPositions.getValue().isEmpty()
                        && !firstScrollInitiated) {
                    singleMessagesThreadRecyclerView.scrollToPosition(
                            searchPositions.getValue().get(searchPositions.getValue().size() -1));
                    firstScrollInitiated = true;
                }
                else if(linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    singleMessagesThreadRecyclerView.scrollToPosition(0);
                }
                return null;
            }
        });

        if(this.threadedConversations != null) {
            if(getIntent().hasExtra(SEARCH_STRING)) {
                conversationsViewModel.threadId = threadedConversations.getThread_id();
                findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);
                String searching = getIntent().getStringExtra(SEARCH_STRING);
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        searchForInput(searching);
                    }
                });
                configureSearchBox();
                searchPositions.setValue(new ArrayList<>(
                        Collections.singletonList(
                                getIntent().getIntExtra(SEARCH_INDEX, 0))));
                conversationsViewModel.getSearch(getApplicationContext(),
                                threadedConversations.getThread_id(), searchPositions.getValue())
                        .observe(this, new Observer<PagingData<Conversation>>() {
                            @Override
                            public void onChanged(PagingData<Conversation> conversationPagingData) {
                                conversationsRecyclerAdapter.submitData(getLifecycle(),
                                        conversationPagingData);
                            }
                        });
                broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final String messageId = intent.getStringExtra(Conversation.ID);
                        ThreadingPoolExecutor.executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Conversation conversation = databaseConnector.conversationDao()
                                        .getMessage(messageId);
                                conversation.setRead(true);
                                conversationsViewModel.update(conversation);
                            }
                        });
                    }
                };
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_DELIVER_ACTION);
                intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_DELIVER_ACTION);

                intentFilter.addAction(IncomingTextSMSBroadcastReceiver.SMS_UPDATED_BROADCAST_INTENT);
                intentFilter.addAction(IncomingDataSMSBroadcastReceiver.DATA_UPDATED_BROADCAST_INTENT);

                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                    registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
                else
                    registerReceiver(broadcastReceiver, intentFilter);
            }
            else if(this.threadedConversations.getThread_id()!= null &&
                    !this.threadedConversations.getThread_id().isEmpty()) {
                conversationsViewModel.get(this.threadedConversations.getThread_id())
                        .observe(this, new Observer<PagingData<Conversation>>() {
                            @Override
                            public void onChanged(PagingData<Conversation> smsList) {
                                conversationsRecyclerAdapter.submitData(getLifecycle(), smsList);
                            }
                        });
            }
        }

        conversationsRecyclerAdapter.retryFailedMessage.observe(this, new Observer<Conversation>() {
            @Override
            public void onChanged(Conversation conversation) {
                List<Conversation> list = new ArrayList<>();
                list.add(conversation);
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        conversationsViewModel.deleteItems(getApplicationContext(), list);
                        try {
                            sendTextMessage(conversation, threadedConversations, conversation.getMessage_id());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        conversationsRecyclerAdapter.retryFailedDataMessage.observe(this, new Observer<Conversation>() {
            @Override
            public void onChanged(Conversation conversation) {
                List<Conversation> list = new ArrayList<>();
                list.add(conversation);
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        conversationsViewModel.deleteItems(getApplicationContext(), list);
                        try {
                            sendDataMessage(threadedConversations);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        conversationsRecyclerAdapter.mutableSelectedItems.observe(this,
                new Observer<HashMap<Long, ConversationTemplateViewHandler>>() {
                    @Override
                    public void onChanged(HashMap<Long, ConversationTemplateViewHandler> selectedItems) {
                        if(selectedItems == null || selectedItems.isEmpty()) {
                            if(actionMode != null) {
                                actionMode.finish();
                            }
                            return;
                        }
                        else if(actionMode == null) {
                            actionMode = startSupportActionMode(actionModeCallback);
                        }
                        if(selectedItems.size() > 1 && actionMode != null)
                            actionMode.invalidate();
                        if(actionMode != null)
                            actionMode.setTitle(String.valueOf(selectedItems.size()));
                    }
                });

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    private void configureSearchBox() {
//        findViewById(R.id.conversations_pop_ups_layouts).setVisibility(View.VISIBLE);
        findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);
        actionMode = startSupportActionMode(searchActionModeCallback);
    }

    private void configureToolbars() {
        setTitle(getAbTitle());
        if(!isShortCode)
            getSupportActionBar().setSubtitle(getAbSubTitle());

    }

    private String getAbTitle() {
        String abTitle = getIntent().getStringExtra(Conversation.ADDRESS);
        if(this.threadedConversations == null || this.threadedConversations.getContact_name() == null) {
            this.threadedConversations.setContact_name(
                    Contacts.retrieveContactName(getApplicationContext(), abTitle));
        }
        return (this.threadedConversations.getContact_name() != null &&
                !this.threadedConversations.getContact_name().isEmpty()) ?
                this.threadedConversations.getContact_name(): this.threadedConversations.getAddress();
    }
    private String getAbSubTitle() {
//        return this.threadedConversations != null &&
//                this.threadedConversations.getAddress() != null ?
//                this.threadedConversations.getAddress(): "";
        if(Contacts.isMuted(getApplicationContext(), threadedConversations.getAddress()))
            return getString(R.string.conversation_menu_mute);
        return "";
    }

    boolean isShortCode = false;

    @Override
    protected void onPause() {
        super.onPause();
        if (smsTextView.getText() != null && !smsTextView.getText().toString().isEmpty()) {
            try {
                saveDraft(String.valueOf(System.currentTimeMillis()),
                        smsTextView.getText().toString(), threadedConversations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }

    static final String DRAFT_TEXT = "DRAFT_TEXT";
    static final String DRAFT_ID = "DRAFT_ID";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state.
        savedInstanceState.putString(DRAFT_TEXT, smsTextView.getText().toString());
        savedInstanceState.putString(DRAFT_ID, String.valueOf(System.currentTimeMillis()));

        // Always call the superclass so it can save the view hierarchy state.
        super.onSaveInstanceState(savedInstanceState);
    }

    private void emptyDraft(){
        conversationsViewModel.clearDraft(getApplicationContext());
    }

    SmsManager smsManager = SmsManager.getDefault();
    public String getSMSCount(String text) {
        final List<String> messages = smsManager.divideMessage(text);
        final int segmentCount = messages.get(messages.size() -1).length();
        return segmentCount +"/"+messages.size();
    }

    private void configureMessagesTextBox() {
        if (mutableLiveDataComposeMessage.getValue() == null ||
                mutableLiveDataComposeMessage.getValue().isEmpty())
            findViewById(R.id.conversation_send_btn).setVisibility(View.INVISIBLE);

        TextView counterView = findViewById(R.id.conversation_compose_text_counter);
        View sendBtn = findViewById(R.id.conversation_send_btn);
        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                int visibility = View.GONE;
                if(!s.isEmpty()) {
                    counterView.setText(getSMSCount(s));
                    visibility = View.VISIBLE;
                }
                TextView dualSimCardName =
                        (TextView) findViewById(R.id.conversation_compose_dual_sim_send_sim_name);
                if(SIMHandler.isDualSim(getApplicationContext())) {
                    dualSimCardName.setVisibility(View.VISIBLE);
                }
                sendBtn.setVisibility(visibility);
                counterView.setVisibility(visibility);
            }
        });

        smsTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if ((motionEvent.getAction() & MotionEvent.ACTION_UP) != 0 &&
                        (motionEvent.getActionMasked() & MotionEvent.ACTION_UP) != 0) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        });

        findViewById(R.id.conversation_send_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(smsTextView.getText() != null && defaultSubscriptionId.getValue() != null) {
                        final String text = smsTextView.getText().toString();
                        sendTextMessage(text, defaultSubscriptionId.getValue(),
                                threadedConversations, String.valueOf(System.currentTimeMillis()),
                                null);
                        smsTextView.setText(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        smsTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mutableLiveDataComposeMessage.setValue(s.toString());
            }
        });


        // Message has been shared from another app to send by SMS
        if (getIntent().hasExtra(Conversation.SHARED_SMS_BODY)) {
            smsTextView.setText(getIntent().getStringExtra(Conversation.SHARED_SMS_BODY));
            getIntent().removeExtra(Conversation.SHARED_SMS_BODY);
        }

        try {
            checkDrafts();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDrafts() throws InterruptedException {
        if(smsTextView.getText() == null || smsTextView.getText().toString().isEmpty())
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Conversation conversation =
                                conversationsViewModel.fetchDraft();
                        if (conversation != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    smsTextView.setText(conversation.getText());
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        emptyDraft();
                    }
                }
            });
    }

    private void configureLayoutForMessageType() {
        if(isShortCode) {
            // Cannot reply to message
            ConstraintLayout smsLayout = findViewById(R.id.compose_message_include_layout);
            smsLayout.setVisibility(View.INVISIBLE);

            Snackbar shortCodeSnackBar = Snackbar.make(findViewById(R.id.conversation_coordinator_layout),
                    getString(R.string.conversation_shortcode_description), Snackbar.LENGTH_INDEFINITE);

//            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), R.style.Theme_main);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.conversation_shortcode_learn_more_text))
                    .setNegativeButton(getString(R.string.conversation_shortcode_learn_more_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
            AlertDialog dialog = builder.create();
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.show();
                }
            };
            shortCodeSnackBar.setAction(getString(R.string.conversation_shortcode_action_button),
                    onClickListener);
            shortCodeSnackBar.show();
        }
    }

    private void blockContact() {
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                threadedConversations.setIs_blocked(true);
                databaseConnector.threadedConversationsDao().update(threadedConversations);
            }
        });

        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                threadedConversations.getAddress());
        Uri uri = getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                contentValues);

        Toast.makeText(getApplicationContext(), getString(R.string.conversations_menu_block_toast),
                Toast.LENGTH_SHORT).show();
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        startActivity(telecomManager.createManageBlockedNumbersIntent(), null);
        finish();
    }


    private void shareItem() {
        Set<Map.Entry<Long, ConversationTemplateViewHandler>> entry =
                conversationsRecyclerAdapter.mutableSelectedItems.getValue().entrySet();
        String text = entry.iterator().next().getValue().getText();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        // Only use for components you have control over
        ComponentName[] excludedComponentNames = {
                new ComponentName(BuildConfig.APPLICATION_ID, ComposeNewMessageActivity.class.getName())
        };
        shareIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponentNames);
        startActivity(shareIntent);
    }

    private void copyItem() {
        Set<Map.Entry<Long, ConversationTemplateViewHandler>> entry =
                conversationsRecyclerAdapter.mutableSelectedItems.getValue().entrySet();
        String text = entry.iterator().next().getValue().getText();
        ClipData clip = ClipData.newPlainText(text, text);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getApplicationContext(), getString(R.string.conversation_copied),
                Toast.LENGTH_SHORT).show();
    }

    private void deleteItems() throws Exception {
        List<Conversation> conversationList = new ArrayList<>();
        for(ConversationTemplateViewHandler viewHandler :
                conversationsRecyclerAdapter.mutableSelectedItems.getValue().values()) {
            Conversation conversation = new Conversation();
            conversation.setId(viewHandler.getId());
            conversation.setMessage_id(viewHandler.getMessage_id());
            conversationList.add(conversation);
        }
        conversationsViewModel.deleteItems(getApplicationContext(), conversationList);
    }

    private void searchForInput(String search){
        conversationsRecyclerAdapter.searchString = search;
        try {
            searchPositions.postValue(conversationsViewModel.search(search));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void viewDetailsPopUp() throws InterruptedException {
        Set<Map.Entry<Long, ConversationTemplateViewHandler>> entry =
                conversationsRecyclerAdapter.mutableSelectedItems.getValue().entrySet();
        String messageId = entry.iterator().next().getValue().getMessage_id();

        StringBuilder detailsBuilder = new StringBuilder();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.conversation_menu_view_details_title))
                .setMessage(detailsBuilder);

        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Conversation conversation = conversationsViewModel.fetch(messageId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            detailsBuilder.append(getString(R.string.conversation_menu_view_details_type))
                                    .append(!conversation.getText().isEmpty() ?
                                            getString(R.string.conversation_menu_view_details_type_text):
                                            getString(R.string.conversation_menu_view_details_type_data))
                                    .append("\n")
                                    .append(conversation.getType() == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ?
                                            getString(R.string.conversation_menu_view_details_from) :
                                            getString(R.string.conversation_menu_view_details_to))
                                    .append(conversation.getAddress())
                                    .append("\n")
                                    .append(getString(R.string.conversation_menu_view_details_sent))
                                    .append(conversation.getType() == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ?
                                            Helpers.formatLongDate(Long.parseLong(conversation.getDate_sent())) :
                                            Helpers.formatLongDate(Long.parseLong(conversation.getDate())));
                            if(conversation.getType() == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ) {
                                detailsBuilder.append("\n")
                                        .append(getString(R.string.conversation_menu_view_details_received))
                                        .append(Helpers.formatLongDate(Long.parseLong(conversation.getDate())));
                            }

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SEND_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                Toast.makeText(this, "Let's do this!!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    TextInputEditText textInputEditText;
    private final ActionMode.Callback searchActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Objects.requireNonNull(getSupportActionBar()).hide();

            View viewGroup = getLayoutInflater().inflate(R.layout.conversation_search_bar_layout,
                    null);
            mode.setCustomView(viewGroup);

//            MenuInflater inflater = mode.getMenuInflater();
//            inflater.inflate(R.menu.conversations_menu_search, menu);
//
//            MenuItem searchItem = menu.findItem(R.id.conversations_search_active);
//            searchItem.expandActionView();

            String searchString = getIntent().getStringExtra(SEARCH_STRING);
            getIntent().removeExtra(SEARCH_STRING);

            textInputEditText = viewGroup.findViewById(R.id.conversation_search_input);
            textInputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if(editable != null && editable.length() > 1) {
                        conversationsRecyclerAdapter.searchString = editable.toString();
                        conversationsRecyclerAdapter.resetSearchItems(searchPositions.getValue());
                        ThreadingPoolExecutor.executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                searchForInput(editable.toString());
                            }
                        });
                    }
                    else {
                        conversationsRecyclerAdapter.searchString = null;
                        if(actionMode != null)
                            actionMode.finish();
                    }
                }
            });
            textInputEditText.setText(searchString);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        // Called when the user exits the action mode.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            toolbar.setVisibility(View.VISIBLE);
            resetSearch();
        }
    };
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Objects.requireNonNull(getSupportActionBar()).hide();
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.conversations_menu_item_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if(Objects.requireNonNull(conversationsRecyclerAdapter.mutableSelectedItems.getValue()).size() > 1) {
                menu.clear();
                mode.getMenuInflater().inflate(R.menu.conversations_menu_items_selected, menu);
                return true;
            }
            return false; // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (R.id.conversations_menu_copy == id) {
                copyItem();
                if(actionMode != null)
                    actionMode.finish();
                return true;
            }
            else if (R.id.conversations_menu_share == id) {
                shareItem();
                if(actionMode != null)
                    actionMode.finish();
                return true;
            }
            else if (R.id.conversations_menu_delete == id ||
                    R.id.conversations_menu_delete_multiple == id) {
                try {
                    deleteItems();
                    if(actionMode != null)
                        actionMode.finish();
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            else if (R.id.conversations_menu_view_details == id) {
                try {
                    viewDetailsPopUp();
                    if(actionMode != null)
                        actionMode.finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            return false;
        }

        // Called when the user exits the action mode.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Objects.requireNonNull(getSupportActionBar()).show();
            actionMode = null;
            conversationsRecyclerAdapter.resetAllSelectedItems();
        }
    };

}