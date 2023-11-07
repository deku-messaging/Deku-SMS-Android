package com.afkanerd.deku.DefaultSMS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationsHandler;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationTemplateViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class ConversationActivity extends DualSIMConversationActivity {
    public static final String COMPRESSED_IMAGE_BYTES = "COMPRESSED_IMAGE_BYTES";
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String SEARCH_STRING = "SEARCH_STRING";
    public static final String SEARCH_OFFSET = "search_offset";
    public static final String SEARCH_POSITION = "search_position";
    public static final String SMS_SENT_INTENT = "SMS_SENT";
    public static final String SMS_DELIVERED_INTENT = "SMS_DELIVERED";
    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    private final int RESULT_GALLERY = 100;
    ConversationsRecyclerAdapter conversationsRecyclerAdapter;

    ConversationsViewModel conversationsViewModel;
    TextInputEditText smsTextView;
    ConstraintLayout multiSimcardConstraint;
    MutableLiveData<String> mutableLiveDataComposeMessage = new MutableLiveData<>();

    Toolbar toolbar;
    ActionBar ab;

    LinearLayoutManager linearLayoutManager;
    RecyclerView singleMessagesThreadRecyclerView;

    SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    int defaultSubscriptionId;

    String searchString;

    MutableLiveData<List<Integer>> searchPositions = new MutableLiveData<>();

    ImageButton backSearchBtn;
    ImageButton forwardSearchBtn;

    ThreadedConversations threadedConversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        test();

        try {
            setupActivityDependencies();
            instantiateGlobals();
            configureToolbars();
            configureRecyclerView();
            configureMessagesTextBox();
            configureBroadcastListeners(conversationsViewModel);

            configureLayoutForMessageType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test() {
//        if(BuildConfig.DEBUG)
//            getIntent().putExtra(SEARCH_STRING, "Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        conversationsViewModel.updateToRead(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversations_menu, menu);
        if (this.threadedConversations.isIs_shortcode()) {
            // menu.setGroupVisible(R.id.default_menu_items, false);
            menu.findItem(R.id.make_call).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private boolean isSearchActive() {
        int visibility = findViewById(R.id.conversations_search_results_found).getVisibility();
        return visibility == View.VISIBLE;
    }

    private void resetSearch() {
        findViewById(R.id.conversations_search_results_found).setVisibility(View.GONE);
        findViewById(R.id.conversations_search_box_layout).setVisibility(View.GONE);
        conversationsRecyclerAdapter.searchString = null;
//        conversationsViewModel.informNewItemChanges(getApplicationContext());
//        conversationsRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home &&
                (conversationsRecyclerAdapter.mutableSelectedItems.getValue() != null &&
                        !conversationsRecyclerAdapter.mutableSelectedItems.getValue().isEmpty())) {
            conversationsRecyclerAdapter.unselectAllItems();
            return true;
        }

        if(isSearchActive()) {
            resetSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupActivityDependencies() throws Exception {
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
            if (sendToString != null && (sendToString.contains("smsto:") || sendToString.contains("sms:"))) {
                String _address = sendToString.substring(sendToString.indexOf(':') + 1);
                _address = Helpers.formatPhoneNumbers(getApplicationContext(), _address);
                Log.d(getLocalClassName(), "Shared address: " + _address);
                getIntent().putExtra(Conversation.ADDRESS, _address);
                getIntent().setAction(null);
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
            Log.d(getLocalClassName(), "Address available!");
            ThreadedConversations threadedConversations = new ThreadedConversations();
            threadedConversations.setAddress(getIntent().getStringExtra(Conversation.ADDRESS));
            this.threadedConversations = ThreadedConversationsHandler.get(getApplicationContext(),
                    threadedConversations);
            if(this.threadedConversations == null) {
                this.threadedConversations = threadedConversations;
            }
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
//                if (conversationsRecyclerAdapter.hasSelectedItems()) {
//                    conversationsRecyclerAdapter.resetAllSelectedItems();
//                }
                if(isSearchActive()) {
                    resetSearch();
                }
                finish();
            }
        });

    }

    int searchPointerPosition;
    TextView searchFoundTextView;

    private void scrollRecyclerViewSearch(int position) {
        if(position == -2){
            String text = "0/0 " + getString(R.string.conversations_search_results_found);
            searchFoundTextView.setText(text);
            return;
        }

        singleMessagesThreadRecyclerView.scrollToPosition(position);
        String text = (searchPointerPosition == -1 ?
                0 :
                searchPointerPosition + 1) + "/" + searchPositions.getValue().size() + " " + getString(R.string.conversations_search_results_found);
        searchFoundTextView.setText(text);
    }

    private void instantiateGlobals() throws GeneralSecurityException, IOException {
        toolbar = (Toolbar) findViewById(R.id.send_smsactivity_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();
        searchFoundTextView = findViewById(R.id.conversations_search_results_found_counter_text);

        backSearchBtn = findViewById(R.id.conversation_search_found_back_btn);
        forwardSearchBtn = findViewById(R.id.conversation_search_found_forward_btn);

        smsTextView = findViewById(R.id.sms_text);
        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);

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
                searchPointerPosition = 0;
                if(!integers.isEmpty()) {
                    scrollRecyclerViewSearch(searchPositions.getValue().get(searchPointerPosition));
                } else {
                    conversationsRecyclerAdapter.searchString = null;
                    scrollRecyclerViewSearch(-2);
                }
            }
        });

        try {
            // TODO should work on this as the SMS does not open in real time
            defaultSubscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
        } catch(Exception e ) {
            e.printStackTrace();
        }
    }

    ConversationDao conversationDao;
    private void configureRecyclerView() throws InterruptedException {
        singleMessagesThreadRecyclerView.setAdapter(conversationsRecyclerAdapter);
        singleMessagesThreadRecyclerView.setItemViewCacheSize(500);
        conversationDao = Conversation.getDao(getApplicationContext());

        conversationsRecyclerAdapter.addOnPagesUpdatedListener(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                if(conversationsRecyclerAdapter.getItemCount() < 1)
                    finish();
                return null;
            }
        });

        if(this.threadedConversations != null) {
            if(getIntent().hasExtra(SEARCH_STRING)) {
                conversationsViewModel.getForSearch(conversationDao, this.threadedConversations.getThread_id())
                        .observe(this, new Observer<PagingData<Conversation>>() {
                            @Override
                            public void onChanged(PagingData<Conversation> smsList) {
                                conversationsRecyclerAdapter.submitData(getLifecycle(), smsList);
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

        conversationsRecyclerAdapter.retryFailedMessage.observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(String[] strings) {
                // TODO
            }
        });

        conversationsRecyclerAdapter.retryFailedDataMessage.observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(String[] strings) {
                // TODO
            }
        });

        try {
            conversationsRecyclerAdapter.mutableSelectedItems.observe(this,
                    new Observer<HashMap<Long, ConversationTemplateViewHandler>>() {
                @Override
                public void onChanged(HashMap<Long, ConversationTemplateViewHandler> selectedItems) {
                    changeToolbarsItemSelected(selectedItems);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(getIntent().hasExtra(SEARCH_STRING)) {
            configureSearchBox();
            TextInputEditText textInputEditText = findViewById(R.id.conversations_search_box);
            textInputEditText.setText(getIntent().getStringExtra(SEARCH_STRING));
            getIntent().removeExtra(SEARCH_STRING);
        }

    }

    private void configureSearchBox() {
        TextInputLayout textInputLayout = findViewById(R.id.conversations_search_box_layout);
        textInputLayout.setVisibility(View.VISIBLE);

        findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);

        TextInputEditText textInputEditText = findViewById(R.id.conversations_search_box);
        scrollRecyclerViewSearch(-2);
        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable != null && editable.length() > 1) {
                    conversationsRecyclerAdapter.searchString = editable.toString();
                    resetPreviousSelections();
                    searchForInput(editable.toString());
                }
                else {
                    conversationsRecyclerAdapter.searchString = null;
                    resetPreviousSelections();
                    searchPositions.setValue(new ArrayList<>());
                }
            }
        });
    }

    private void resetPreviousSelections() {
        final List<Integer> prevPositions = searchPositions.getValue();
        if(prevPositions != null)
            for(Integer position : prevPositions) {
                conversationsRecyclerAdapter.notifyItemChanged(position);
            }
    }

    private void configureToolbars() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (R.id.copy == id) {
                    copyItem();
                    conversationsRecyclerAdapter.unselectAllItems();
                    return true;
                }
                else if (R.id.delete == id || R.id.delete_multiple == id) {
                    try {
                        deleteItems();
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                else if (R.id.make_call == id) {
                    ThreadedConversationsHandler.call(getApplicationContext(), threadedConversations);
                    return true;
                }
                else if(R.id.search_conversations == id) {
//                    configureSearchBox();
                    Intent intent = new Intent(getApplicationContext(), SearchMessagesThreadsActivity.class);
                    intent.putExtra(Conversation.THREAD_ID, threadedConversations.getThread_id());
                    startActivity(intent);
                }
                return false;
            }
        });

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(getAbTitle());
        ab.setSubtitle(getAbSubTitle());
    }

    private String getAbTitle() {
        String abTitle = getIntent().getStringExtra(Conversation.ADDRESS);
        if(this.threadedConversations != null)
            abTitle = (this.threadedConversations.getContact_name() != null &&
                    !this.threadedConversations.getContact_name().isEmpty()) ?
                    this.threadedConversations.getContact_name(): this.threadedConversations.getAddress();
        return abTitle;
    }
    private String getAbSubTitle() {
        return (this.threadedConversations != null &&
                this.threadedConversations.getContact_name() != null &&
                !this.threadedConversations.getContact_name().isEmpty()) ?
                this.threadedConversations.getAddress(): "";
    }

    private void configureMessagesTextBox() throws GeneralSecurityException, IOException {
        if (mutableLiveDataComposeMessage.getValue() == null ||
                mutableLiveDataComposeMessage.getValue().isEmpty())
            findViewById(R.id.sms_send_button).setVisibility(View.INVISIBLE);

        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                findViewById(R.id.sms_send_button).setVisibility(s.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            }
        });
        findViewById(R.id.sms_send_button).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickSendButton(v);
                return true;
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

        TextView encryptedMessageTextView = findViewById(R.id.send_sms_encrypted_version);
        encryptedMessageTextView.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.sms_send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendTextMessage(v);
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

                // TODO
//                try {
//                    if (!s.toString().isEmpty() && hasSecretKey) {
//                        String encryptedString = Base64.encodeToString(
//                                SecurityAES.encrypt_256_cbc(s.toString().getBytes(StandardCharsets.UTF_8),
//                                        secretKey, null),
//                                Base64.DEFAULT);
//
//                        encryptedString = SecurityHelpers.putEncryptedMessageWaterMark(encryptedString);
//                        String stats = SMSHandler.calculateSMS(encryptedString);
//                        String displayedString = encryptedString + "\n\n" + stats;
//
//                        encryptedMessageTextView.setVisibility(View.VISIBLE);
//                        encryptedMessageTextView.setText(displayedString);
//                        if (encryptedMessageTextView.getLayout() != null)
//                            encryptedMessageTextView.scrollTo(0,
//                                    encryptedMessageTextView.getLayout().getLineTop(
//                                            encryptedMessageTextView.getLineCount()) - encryptedMessageTextView.getHeight());
//                    } else {
//                        encryptedMessageTextView.setVisibility(View.GONE);
//                    }
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
            }
        });

        // Message has been shared from another app to send by SMS
        if (getIntent().hasExtra(Conversation.SHARED_SMS_BODY)) {
            smsTextView.setText(getIntent().getStringExtra(Conversation.SHARED_SMS_BODY));
            getIntent().removeExtra(Conversation.SHARED_SMS_BODY);

            // TODO
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mutableLiveDataComposeMessage
//                            .setValue(getIntent().getStringExtra(Conversation.SHARED_SMS_BODY));
//                }
//            });
        }
    }

    private void configureLayoutForMessageType() {
        if(this.threadedConversations != null && this.threadedConversations.isIs_shortcode()) {
            // Cannot reply to message
            ConstraintLayout smsLayout = findViewById(R.id.send_message_content_layouts);
            smsLayout.setVisibility(View.GONE);
        }
    }


    private void changeToolbarsItemSelected(HashMap<Long, ConversationTemplateViewHandler> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            showDefaultToolbar(toolbar.getMenu());
        } else {
            hideDefaultToolbar(toolbar.getMenu(), selectedItems.size());
        }
    }

    public void sendTextMessage(View view) throws Exception {
        if(smsTextView.getText() != null) {
            String text = smsTextView.getText().toString();
            // TODO: encryption

            String[] transmissionOutput = _sendSMSMessage(defaultSubscriptionId, text);

            String messageId = transmissionOutput[NativeSMSDB.MESSAGE_ID];
            String threadId = transmissionOutput[NativeSMSDB.THREAD_ID];

            conversationsViewModel.insertFromNative(getApplicationContext(), messageId);
            if(this.threadedConversations == null) {
                getIntent().putExtra(Conversation.THREAD_ID, threadId);
                setupActivityDependencies();
            }

            smsTextView.getText().clear();
        }
    }

    private String[] _sendSMSMessage(int subscriptionId, String text) {
        try {
            String address = this.threadedConversations == null ?
                    getIntent().getStringExtra(Conversation.ADDRESS) :
                    this.threadedConversations.getAddress();
            return NativeSMSDB.Outgoing.send_text(getApplicationContext(), address, text,
                    subscriptionId, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void hideDefaultToolbar(Menu menu, int size) {
        Log.d(getLocalClassName(), "Got called to work: " + size);
        menu.setGroupVisible(R.id.default_menu_items, false);
        if (size > 1) {
            menu.setGroupVisible(R.id.single_message_copy_menu, false);
            menu.setGroupVisible(R.id.multiple_message_copy_menu, true);
        } else {
            menu.setGroupVisible(R.id.multiple_message_copy_menu, false);
            menu.setGroupVisible(R.id.single_message_copy_menu, true);
        }

        ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
        ab.setTitle(String.valueOf(size));

        ab.setSubtitle("");
    }

    private void showDefaultToolbar(Menu menu) {
        // TODO
//        if(!conversationHandler.isShortCode())
//            menu.setGroupVisible(R.id.default_menu_items, true);
        menu.setGroupVisible(R.id.default_menu_items, true);
        menu.setGroupVisible(R.id.single_message_copy_menu, false);

        ab.setHomeAsUpIndicator(null);
        ab.setTitle(getAbTitle());
        ab.setSubtitle(getAbSubTitle());
    }

    private void copyItem() {
        // TODO
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
        conversationsRecyclerAdapter.mutableSelectedItems.setValue(null);
        conversationsViewModel.deleteItems(getApplicationContext(), conversationList);
    }

    private void searchForInput(String search){
        try {
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

}