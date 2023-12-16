package com.afkanerd.deku.DefaultSMS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsHandler;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationTemplateViewHandler;
import com.afkanerd.deku.E2EE.E2EECompactActivity;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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
    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    ActionMode actionMode;
    ConversationsRecyclerAdapter conversationsRecyclerAdapter;

    ConversationsViewModel conversationsViewModel;
    TextInputEditText smsTextView;
    MutableLiveData<String> mutableLiveDataComposeMessage = new MutableLiveData<>();

    LinearLayoutManager linearLayoutManager;
    RecyclerView singleMessagesThreadRecyclerView;


    String searchString;

    MutableLiveData<List<Integer>> searchPositions = new MutableLiveData<>();

    ImageButton backSearchBtn;
    ImageButton forwardSearchBtn;

    ThreadedConversations threadedConversations;

    Toolbar toolbar;

    private String draftMessageId;
    private String draftText;


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
            setViewModel(conversationsViewModel);

            configureLayoutForMessageType();
            configureBroadcastListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy.
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance.
        smsTextView.setText(savedInstanceState.getString(DRAFT_TEXT));
        draftMessageId = savedInstanceState.getString(DRAFT_ID);

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
        if(this.conversationsViewModel != null) {
            conversationsViewModel.updateToRead(getApplicationContext());
            Intent intent = new Intent(DRAFT_PRESENT_BROADCAST);
            sendBroadcast(intent);
        }

        TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
        layout.requestFocus();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                            E2EEHandler.getKeyStoreAlias(threadedConversations.getAddress(),
                                    0) )) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layout.setPlaceholderText(getString(R.string.send_message_secured_text_box_hint));
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
//        if(isSearchActive()) {
//            resetSearch();
//            return true;
//        }
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
            this.threadedConversations = ThreadedConversationsHandler.get(getApplicationContext(),
                    threadedConversations);
        }
        else if(getIntent().hasExtra(Conversation.ADDRESS)) {
            ThreadedConversations threadedConversations = new ThreadedConversations();
            threadedConversations.setAddress(getIntent().getStringExtra(Conversation.ADDRESS));
            this.threadedConversations = ThreadedConversationsHandler.get(getApplicationContext(),
                    threadedConversations);
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

        conversationsRecyclerAdapter = new ConversationsRecyclerAdapter(getApplicationContext());

        conversationsViewModel = new ViewModelProvider(this)
                .get(ConversationsViewModel.class);

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

    ConversationDao conversationDao;
    boolean firstScrollInitiated = false;

    LifecycleOwner lifecycleOwner;
    private void configureRecyclerView() throws InterruptedException {
        singleMessagesThreadRecyclerView.setAdapter(conversationsRecyclerAdapter);
        singleMessagesThreadRecyclerView.setItemViewCacheSize(500);
        conversationDao = Conversation.getDao(getApplicationContext());

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
                conversationsViewModel.conversationDao = conversationDao;
                conversationsViewModel.threadId = threadedConversations.getThread_id();
                findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);
                String searching = getIntent().getStringExtra(SEARCH_STRING);
//                List<Integer> positions = conversationsViewModel.search(searching);
//                searchPositions.setValue(positions);
                searchForInput(searching);
                configureSearchBox();
                conversationsViewModel.getSearch(conversationDao, this.threadedConversations.getThread_id(),
                                searchPositions.getValue())
                        .observe(this, new Observer<PagingData<Conversation>>() {
                            @Override
                            public void onChanged(PagingData<Conversation> conversationPagingData) {
                                conversationsRecyclerAdapter.submitData(getLifecycle(), conversationPagingData);
                            }
                        });
            }
            else if(this.threadedConversations.getThread_id()!= null &&
                    !this.threadedConversations.getThread_id().isEmpty()) {
                conversationsViewModel.get(conversationDao, this.threadedConversations.getThread_id())
                        .observe(this, new Observer<PagingData<Conversation>>() {
                            @Override
                            public void onChanged(PagingData<Conversation> smsList) {
                                conversationsRecyclerAdapter.submitData(getLifecycle(), smsList);
                            }
                        });
            } else if(this.threadedConversations.getAddress()!= null && !this.threadedConversations.getAddress().isEmpty()) {
                conversationsViewModel.getByAddress(conversationDao, this.threadedConversations.getAddress())
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
                conversationsViewModel.deleteItems(getApplicationContext(), list);
                try {
                    sendTextMessage(conversation, threadedConversations, draftMessageId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        conversationsRecyclerAdapter.retryFailedDataMessage.observe(this, new Observer<Conversation>() {
            @Override
            public void onChanged(Conversation conversation) {
                List<Conversation> list = new ArrayList<>();
                list.add(conversation);
                conversationsViewModel.deleteItems(getApplicationContext(), list);
                try {
                    sendDataMessage(threadedConversations);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        return this.threadedConversations != null &&
                this.threadedConversations.getAddress() != null ?
                this.threadedConversations.getAddress(): "";
    }

    boolean isShortCode = false;

    @Override
    protected void onStop() {
        super.onStop();
        if (draftMessageId != null && !draftText.isEmpty())
            saveDraft(draftMessageId, draftText, threadedConversations);
    }

    static final String DRAFT_TEXT = "DRAFT_TEXT";
    static final String DRAFT_ID = "DRAFT_ID";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state.
        savedInstanceState.putString(DRAFT_TEXT, draftText);
        savedInstanceState.putString(DRAFT_ID, draftMessageId);

        // Always call the superclass so it can save the view hierarchy state.
        super.onSaveInstanceState(savedInstanceState);
    }

    private void emptyDraft(){
        conversationsViewModel.clearDraft();
        draftMessageId = null;
        draftText = null;
    }

    private void configureMessagesTextBox() {
        if (mutableLiveDataComposeMessage.getValue() == null ||
                mutableLiveDataComposeMessage.getValue().isEmpty())
            findViewById(R.id.conversation_send_btn).setVisibility(View.INVISIBLE);

        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                int visibility = s.isEmpty() ? View.INVISIBLE : View.VISIBLE;
                if(simCount > 1) {
                    findViewById(R.id.conversation_compose_dual_sim_send_sim_name)
                            .setVisibility(visibility);
                }
                findViewById(R.id.conversation_send_btn).setVisibility(visibility);

                if(s.isEmpty()) {
                    if(draftMessageId != null) {
                        emptyDraft();
                    }
                } else {
                    if(draftMessageId == null)
                        draftMessageId = String.valueOf(System.currentTimeMillis());
                    draftText = s;
                }
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
                    final String text = smsTextView.getText().toString();
                    sendTextMessage(text, defaultSubscriptionId.getValue(),
                            threadedConversations, draftMessageId);
                    smsTextView.setText(null);
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
        if(draftText != null && draftMessageId != null) {
            smsTextView.setText(draftText);
        }
        else {
            Conversation conversation = conversationsViewModel.fetchDraft();
            if (conversation != null) {
                smsTextView.setText(conversation.getText());
                draftMessageId = conversation.getMessage_id();
            }
        }
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
        try {
            conversationsRecyclerAdapter.searchString = search;
            searchPositions.setValue(conversationsViewModel.search(search));
        } catch(Exception e) {
            e.printStackTrace();
        }
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
                        searchForInput(editable.toString());
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