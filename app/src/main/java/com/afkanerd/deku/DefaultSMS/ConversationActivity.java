package com.afkanerd.deku.DefaultSMS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Fragments.ConversationsContactModalFragment;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsHandler;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationTemplateViewHandler;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.E2EECompactActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.getstream.avatarview.AvatarView;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ConversationActivity extends E2EECompactActivity {
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String SEARCH_STRING = "SEARCH_STRING";
    public static final String SEARCH_INDEX = "SEARCH_INDEX";
    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    boolean isContact = false;
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

    boolean firstScrollInitiated = false;

    int searchPointerPosition;
    TextView searchFoundTextView;

    LifecycleOwner lifecycleOwner;

    static final String DRAFT_TEXT = "DRAFT_TEXT";
    static final String DRAFT_ID = "DRAFT_ID";

    MaterialCardView materialCardView;
    boolean isShortCode = false;
    boolean isDualSim = false;
    SmsManager smsManager = SmsManager.getDefault();
    TextInputEditText textInputEditText;

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

    @Override
    protected void onResume() {
        super.onResume();
        TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
        layout.requestFocus();
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    NativeSMSDB.Incoming.update_read(getApplicationContext(), 1, threadId,
                            null);
                    conversationsViewModel.updateInformation(getApplicationContext(), contactName);
                    emptyDraft();
                } catch (Exception e) {
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
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                ThreadedConversations threadedConversations =
                        databaseConnector.threadedConversationsDao().get(threadId);
                if(threadedConversations != null && threadedConversations.isIs_mute()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            menu.findItem(R.id.conversations_menu_unmute).setVisible(true);
                            menu.findItem(R.id.conversations_menu_mute).setVisible(false);
                        }
                    });
                }
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.conversation_main_menu_call == item.getItemId()) {
            ThreadedConversationsHandler.call(getApplicationContext(), address);
            return true;
        }
        else if(R.id.conversation_main_menu_search == item.getItemId()) {
            Intent intent = new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class);
            intent.putExtra(Conversation.THREAD_ID, threadId);
            startActivity(intent);
        }
        else if (R.id.conversations_menu_block == item.getItemId()) {
            blockContact();
            if(actionMode != null)
                actionMode.finish();
            return true;
        }
        else if (R.id.conversations_menu_delete == item.getItemId()) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.threadedConversationsDao()
                            .delete(getApplicationContext(), Collections.singletonList(threadId));
                }
            });
            finish();
        }
        else if (R.id.conversations_menu_mute == item.getItemId()) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    conversationsViewModel.mute();
                    ThreadedConversations threadedConversations =
                            databaseConnector.threadedConversationsDao().get(threadId);
                    threadedConversations.setIs_mute(true);

                    databaseConnector.threadedConversationsDao().update(getApplicationContext(),
                            threadedConversations);
                    invalidateMenu();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            configureToolbars();
                            Toast.makeText(getApplicationContext(), getString(R.string.conversation_menu_muted),
                                    Toast.LENGTH_SHORT).show();
                            if(actionMode != null)
                                actionMode.finish();
                        }
                    });
                }
            });
            return true;
        }
        else if (R.id.conversations_menu_unmute == item.getItemId()) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    conversationsViewModel.unMute();
                    ThreadedConversations threadedConversations =
                            databaseConnector.threadedConversationsDao().get(threadId);
                    threadedConversations.setIs_mute(false);
                    databaseConnector.threadedConversationsDao().update(getApplicationContext(),
                            threadedConversations);

                    invalidateMenu();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            configureToolbars();
                            Toast.makeText(getApplicationContext(), getString(R.string.conversation_menu_unmuted),
                                    Toast.LENGTH_SHORT).show();
                            if(actionMode != null)
                                actionMode.finish();
                        }
                    });
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (smsTextView != null && smsTextView.getText() != null &&
                !smsTextView.getText().toString().isEmpty()) {
            try {
                saveDraft(String.valueOf(System.currentTimeMillis()),
                        smsTextView.getText().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state.
        savedInstanceState.putString(DRAFT_TEXT, smsTextView.getText().toString());
        savedInstanceState.putString(DRAFT_ID, String.valueOf(System.currentTimeMillis()));

        // Always call the superclass so it can save the view hierarchy state.
        super.onSaveInstanceState(savedInstanceState);
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


    private void resetSearch() {
        findViewById(R.id.conversations_search_results_found).setVisibility(View.GONE);
        conversationsRecyclerAdapter.searchString = null;
        conversationsRecyclerAdapter.resetSearchItems(searchPositions.getValue());
        searchPositions = new MutableLiveData<>();
    }

    String defaultRegion;
    private void configureActivityDependencies() throws Exception {
        /**
         * Address = This could come from Shared Intent, Contacts etc
         * ThreadID = This comes from Thread screen and notifications
         * ThreadID is the intended way of populating the messages
         * ==> If not ThreadId do not populate, everything else should take the pleasure of finding
         * and sending a threadID to this intent
         */
        defaultRegion = Helpers.getUserCountry(getApplicationContext());
        if(getIntent().getAction() != null && (getIntent().getAction().equals(Intent.ACTION_SENDTO) ||
                getIntent().getAction().equals(Intent.ACTION_SEND))) {
            String sendToString = getIntent().getDataString();
            if (sendToString != null && (sendToString.contains("smsto:") ||
                    sendToString.contains("sms:"))) {
                String address = Helpers.getFormatCompleteNumber(sendToString, defaultRegion);
                getIntent().putExtra(Conversation.ADDRESS, address);
            }
        }

        if(!getIntent().hasExtra(Conversation.THREAD_ID) &&
                !getIntent().hasExtra(Conversation.ADDRESS)) {
            throw new Exception("No threadId nor Address supplied for activity");
        }
        if(getIntent().hasExtra(Conversation.THREAD_ID)) {
            threadId = getIntent().getStringExtra(Conversation.THREAD_ID);
        }
        if(getIntent().hasExtra(Conversation.ADDRESS)) {
            address = getIntent().getStringExtra(Conversation.ADDRESS);
        }

        if(threadId == null)
            threadId = ThreadedConversationsHandler.get(getApplicationContext(), address)
                    .getThread_id();
        if(address == null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ThreadedConversations threadedConversations =
                            databaseConnector.threadedConversationsDao().get(threadId);
                    address = threadedConversations.getAddress();
                }
            });
            thread.start();
            thread.join();
        }
        contactName = Contacts.retrieveContactName(getApplicationContext(),
                Helpers.getFormatCompleteNumber(address, defaultRegion));
        if(contactName == null) {
            contactName = Helpers.getFormatForTransmission(address, defaultRegion);
        } else isContact = true;

        isShortCode = Helpers.isShortCode(address);
        attachObservers();

        isDualSim = SIMHandler.isDualSim(getApplicationContext());
    }

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

        conversationsRecyclerAdapter = new ConversationsRecyclerAdapter();

        conversationsViewModel = new ViewModelProvider(this)
                .get(ConversationsViewModel.class);
        conversationsViewModel.datastore = databaseConnector;

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

    private void configureRecyclerView() throws InterruptedException {
        singleMessagesThreadRecyclerView.setAdapter(conversationsRecyclerAdapter);
//        singleMessagesThreadRecyclerView.setItemViewCacheSize(500);

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

        if(getIntent().hasExtra(SEARCH_STRING)) {
            conversationsViewModel.threadId = threadId;
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
            conversationsViewModel.getSearch(getApplicationContext(), threadId,
                            searchPositions.getValue())
                    .observe(this, new Observer<PagingData<Conversation>>() {
                        @Override
                        public void onChanged(PagingData<Conversation> conversationPagingData) {
                            conversationsRecyclerAdapter.submitData(getLifecycle(),
                                    conversationPagingData);
                        }
                    });
        }
        conversationsViewModel.get(threadId).observe(this,
                new Observer<PagingData<Conversation>>() {
                    @Override
                    public void onChanged(PagingData<Conversation> smsList) {
                        conversationsRecyclerAdapter.submitData(getLifecycle(), smsList);
                    }
                });

        conversationsRecyclerAdapter.retryFailedMessage.observe(this, new Observer<Conversation>() {
            @Override
            public void onChanged(Conversation conversation) {
                List<Conversation> list = new ArrayList<>();
                list.add(conversation);

                // TODO: make this call a modal sheet and work from there
                ThreadingPoolExecutor.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        conversationsViewModel.deleteItems(getApplicationContext(), list);
                        try {
                            ThreadedConversations threadedConversations =
                                    databaseConnector.threadedConversationsDao().get(threadId);
                            sendTextMessage(conversation, threadedConversations,
                                    conversation.getMessage_id());
                        } catch (Exception e) {
                            Log.e(getClass().getName(), "Exception sending failed message", e);
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
                            ThreadedConversations threadedConversations =
                                    databaseConnector.threadedConversationsDao().get(threadId);
                            sendDataMessage(threadedConversations);
                        } catch (Exception e) {
                            Log.e(getClass().getName(),
                                    "Exception sending failed data message", e);
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

    private void configureSearchBox() {
//        findViewById(R.id.conversations_pop_ups_layouts).setVisibility(View.VISIBLE);
        findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);
        actionMode = startSupportActionMode(searchActionModeCallback);
    }

    private void configureToolbars() {
        setTitle(null);
//        View view = findViewById(R.id.conversation_toolbar_include_contact_card);
        TextView contactTextView = findViewById(R.id.conversation_contact_card_text_view);
        contactTextView.setText(getAbTitle());

        AvatarView avatarView = findViewById(R.id.conversation_contact_card_frame_avatar_initials);
        ImageView imageView = findViewById(R.id.conversation_contact_card_frame_avatar_photo);
        final int contactColor = Helpers.getColor(getApplicationContext(), threadId);
        if(isContact) {
            avatarView.setAvatarInitials(contactName.contains(" ") ? contactName :
                    contactName.substring(0, 1));
            avatarView.setAvatarInitialsBackgroundColor(contactColor);
            imageView.setVisibility(View.INVISIBLE);
        } else {
            Drawable drawable = getDrawable(R.drawable.baseline_account_circle_24);
            if (drawable != null)
                drawable.setColorFilter(contactColor, PorterDuff.Mode.SRC_IN);
            imageView.setImageDrawable(drawable);
            avatarView.setVisibility(View.INVISIBLE);
        }

//        View view = getLayoutInflater().inflate(R.layout.layout_conversation_contact_card, null);
        materialCardView = findViewById(R.id.conversation_toolbar_contact_card);
        materialCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "Yes contact clicked");
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                ConversationsContactModalFragment modalSheetFragment =
                        new ConversationsContactModalFragment(contactName,
                                Helpers.getFormatForTransmission(address, defaultRegion));
                fragmentTransaction.add(modalSheetFragment, ConversationsContactModalFragment.TAG);
                fragmentTransaction.show(modalSheetFragment);
                fragmentTransaction.commitNow();
            }
        });

    }

    private String getAbTitle() {
        return isContact? contactName: address;
    }

    private void emptyDraft(){
        conversationsViewModel.clearDraft(getApplicationContext());
    }

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

        MaterialTextView dualSimCardName =
                findViewById(R.id.conversation_compose_dual_sim_send_sim_name);
        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(!s.isEmpty()) {
                    counterView.setText(getSMSCount(s));
                    sendBtn.setVisibility(View.VISIBLE);
                    if(isDualSim)
                        dualSimCardName.setVisibility(View.VISIBLE);
                }
                else {
                    sendBtn.setVisibility(View.GONE);
                    dualSimCardName.setVisibility(View.GONE);
                    counterView.setVisibility(View.GONE);
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
                    if(smsTextView.getText() != null && defaultSubscriptionId.getValue() != null) {
                        final String text = smsTextView.getText().toString();
                        ThreadingPoolExecutor.executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                ThreadedConversations threadedConversations =
                                        Datastore.getDatastore(getApplicationContext())
                                                .threadedConversationsDao().get(threadId);
                                if(threadedConversations == null) {
                                    threadedConversations = new ThreadedConversations();
                                    threadedConversations.setThread_id(threadId);
                                }
                                try {
                                    sendTextMessage(text, defaultSubscriptionId.getValue(),
                                            threadedConversations,
                                            String.valueOf(System.currentTimeMillis()),
                                            null);
                                } catch (NumberParseException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
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
                ThreadedConversations threadedConversations =
                        Datastore.getDatastore(getApplicationContext())
                                .threadedConversationsDao().get(threadId);
                threadedConversations.setIs_blocked(true);
                databaseConnector.threadedConversationsDao().update(getApplicationContext(),
                        threadedConversations);
            }
        });

        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, address);
        Uri uri = getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                contentValues);

        Toast.makeText(getApplicationContext(), getString(R.string.conversations_menu_block_toast),
                Toast.LENGTH_SHORT).show();
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        startActivity(telecomManager.createManageBlockedNumbersIntent(), null);
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
                new ComponentName(BuildConfig.APPLICATION_ID,
                        ThreadedConversationsActivity.class.getName())
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

    private final ActionMode.Callback searchActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Objects.requireNonNull(getSupportActionBar()).hide();

            View viewGroup = getLayoutInflater().inflate(R.layout.layout_conversation_search_bar,
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
            if(Objects.requireNonNull(conversationsRecyclerAdapter.mutableSelectedItems
                    .getValue()).size() > 1) {
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