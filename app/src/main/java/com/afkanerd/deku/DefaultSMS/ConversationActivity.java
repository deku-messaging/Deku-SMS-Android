package com.afkanerd.deku.DefaultSMS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Models.Archive.ArchiveHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.DefaultSMS.Settings.SettingsHandler;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversationActivity extends CustomAppCompactActivity {
    public static final String COMPRESSED_IMAGE_BYTES = "COMPRESSED_IMAGE_BYTES";
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String SEARCH_STRING = "search_string";
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

    SMS.SMSMetaEntity smsMetaEntity;

    SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    int defaultSubscriptionId;

    MutableLiveData<List<Integer>> searchPositions = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        try {
            _setupActivityDependencies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            _instantiateGlobals();
            _configureToolbars();
            _configureRecyclerView();
            _configureMessagesTextBox();
        } catch (Exception e) {
            e.printStackTrace();
        }
        configureBroadcastListeners(new Runnable() {
            @Override
            public void run() {
                if(getIntent().hasExtra(SMS.SMSMetaEntity.THREAD_ID)) {
                    if(conversationsViewModel.threadId == null)
                        conversationsViewModel.informNewItemChanges(getApplicationContext(),
                                smsMetaEntity.getThreadId());
                    else
                        conversationsViewModel.informNewItemChanges(getApplicationContext());
//                    cancelNotifications(smsMetaEntity.getThreadId());
                    try {
                        _checkEncryptionStatus();
                    } catch (GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        _configureLayoutForMessageType();
        _configureEncryptionListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(getIntent().hasExtra(SMS.SMSMetaEntity.THREAD_ID))
                    _updateThreadToRead();
            }
        }).start();

        try {
            _checkEncryptionStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversations_menu, menu);
        if (smsMetaEntity.isShortCode())
            menu.setGroupVisible(R.id.default_menu_items, false);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean isSearchActive() {
        int visibility = findViewById(R.id.conversations_search_results_found).getVisibility();
        return visibility == View.VISIBLE;
    }

    private void resetSearch() {
        findViewById(R.id.conversations_search_results_found).setVisibility(View.GONE);
        findViewById(R.id.conversations_search_box_layout).setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && conversationsRecyclerAdapter.hasSelectedItems()) {
            conversationsRecyclerAdapter.resetAllSelectedItems();
            return true;
        }

        if(isSearchActive()) {
            resetSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void _setupActivityDependencies() throws Exception {
        /**
         * Address = This could come from Shared Intent, Contacts etc
         * ThreadID = This comes from Thread screen and notifications
         * ThreadID is the intended way of populating the messages
         * ==> If not ThreadId do not populate, everything else should take the pleasure of finding
         * and sending a threadID to this intent
         */

        smsMetaEntity = new SMS.SMSMetaEntity();
        if(getIntent().getAction() != null && (getIntent().getAction().equals(Intent.ACTION_SENDTO) ||
                getIntent().getAction().equals(Intent.ACTION_SEND))) {
            String sendToString = getIntent().getDataString();
            if (sendToString.contains("smsto:") || sendToString.contains("sms:")) {
                String _address = sendToString.substring(sendToString.indexOf(':') + 1);
                getIntent().putExtra(SMS.SMSMetaEntity.ADDRESS, _address);
            }
        }

        if(!getIntent().hasExtra(SMS.SMSMetaEntity.THREAD_ID) &&
                !getIntent().hasExtra(SMS.SMSMetaEntity.ADDRESS)) {
            throw new Exception("No threadId nor Address supplied for activity");
        }

        if(getIntent().hasExtra(SMS.SMSMetaEntity.THREAD_ID))
            smsMetaEntity.setThreadId(getApplicationContext(),
                    getIntent().getStringExtra(SMS.SMSMetaEntity.THREAD_ID));

        if(getIntent().hasExtra(SMS.SMSMetaEntity.ADDRESS)) {
            String threadId = smsMetaEntity.setAddress(getApplicationContext(),
                    getIntent().getStringExtra(SMS.SMSMetaEntity.ADDRESS));

            if(threadId != null && !threadId.isEmpty()) {
                getIntent().putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                smsMetaEntity.setThreadId(getApplicationContext(), threadId);
            }
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (conversationsRecyclerAdapter.hasSelectedItems()) {
                    conversationsRecyclerAdapter.resetAllSelectedItems();
                }
                else if(isSearchActive()) {
                    resetSearch();
                }
                finish();
            }
        });
    }

    private void _instantiateGlobals() throws GeneralSecurityException, IOException {
        toolbar = (Toolbar) findViewById(R.id.send_smsactivity_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();

        smsTextView = findViewById(R.id.sms_text);
//        multiSimcardConstraint = findViewById(R.id.simcard_select_constraint);
        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);

        conversationsRecyclerAdapter = new ConversationsRecyclerAdapter(getApplicationContext(),
                smsMetaEntity.getAddress());

        conversationsViewModel = new ViewModelProvider(this)
                .get(ConversationsViewModel.class);

//        linearLayoutManager = new LinearLayoutManager(getApplicationContext(),
//                LinearLayoutManager.VERTICAL, true);

        TextView searchFoundTextView = findViewById(R.id.conversations_search_results_found_counter_text);
        searchPositions.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> integers) {
                Log.d(getLocalClassName(), "Search found: " + integers.size());
                if(!integers.isEmpty()) {
                    int requiredScrollPos = integers.get(integers.size() - 1);
                    singleMessagesThreadRecyclerView.scrollToPosition(requiredScrollPos);
                }
                String text = integers.size() + " " + getString(R.string.conversations_search_results_found);
                searchFoundTextView.setText(text);
            }
        });

        try {
            // TODO should work on this as the SMS does not open in real time
            defaultSubscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
        } catch(Exception e ) {
            e.printStackTrace();
        }

        sharedPreferences = getSharedPreferences(SecurityECDH.UNIVERSAL_KEYSTORE_ALIAS, Context.MODE_PRIVATE);
        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // Keys are encrypted so can't check for specifc entries
                try {
                    _checkEncryptionStatus();
                    if(sharedPreferences.contains(key))
                        _configureMessagesTextBox();
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void _configureRecyclerView() {
        singleMessagesThreadRecyclerView.setAdapter(conversationsRecyclerAdapter);

        int offset = getIntent().getIntExtra(SEARCH_OFFSET, 0);

        conversationsViewModel.getMessages(
                getApplicationContext(), smsMetaEntity.getThreadId(), offset).observe(this, new Observer<List<SMS>>() {
            @Override
            public void onChanged(List<SMS> smsList) {
                conversationsRecyclerAdapter.submitList(smsList);
            }
        });

        conversationsRecyclerAdapter.retryFailedMessage.observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(String[] strings) {
                try {
                    if(strings.length < 2)
                        return;

                    smsMetaEntity.deleteMessage(getApplicationContext(), strings[0]);
                    // TODO: make this use the previously used subscription id
                    int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
                    _sendSMSMessage(subscriptionId, strings[1]);
                    conversationsRecyclerAdapter.retryFailedMessage.setValue(new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        conversationsRecyclerAdapter.retryFailedDataMessage.observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(String[] strings) {
                try {
                    if(strings.length < 2)
                        return;

                    // TODO: fix this to send data without it being key
                    smsMetaEntity.deleteMessage(getApplicationContext(), strings[0]);
                    // TODO: make this use the previously used subscription id
                    int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
                    _sendKeyDataMessage(subscriptionId, Base64.decode(strings[1], Base64.DEFAULT));
                    conversationsRecyclerAdapter.retryFailedDataMessage.setValue(new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        singleMessagesThreadRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = smsMetaEntity.fetchMessages(getApplicationContext(), 0, 0);
                        int msgLen = cursor.getCount();
                        cursor.close();

                        if(conversationsRecyclerAdapter.mDiffer.getCurrentList().size() >= msgLen)
                            return;

                        final int maximumScrollPosition = conversationsRecyclerAdapter.getItemCount() - 3;

                        try {
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            if (layoutManager != null) {
                                final int lastTopVisiblePosition = layoutManager.findLastVisibleItemPosition();
                                final int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                                if (!conversationsViewModel.offsetStartedFromZero && firstVisibleItemPosition == 0) {
                                    int newSize = conversationsViewModel.refreshDown(getApplicationContext());

                                    if (newSize > 0)
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.scrollToPosition(lastTopVisiblePosition + 1 + newSize);
                                            }
                                        });
                                } else if (conversationsViewModel.offsetStartedFromZero &&
                                        lastTopVisiblePosition >= maximumScrollPosition && firstVisibleItemPosition > 0) {
                                    conversationsViewModel.refresh(getApplicationContext());
                                    int itemCount = recyclerView.getAdapter().getItemCount();
                                    if (itemCount > maximumScrollPosition + 1)
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.scrollToPosition(lastTopVisiblePosition);
                                            }
                                        });
                                }

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        try {
            conversationsRecyclerAdapter.selectedItem.observe(this, new Observer<HashMap<String, RecyclerView.ViewHolder>>() {
                @Override
                public void onChanged(HashMap<String, RecyclerView.ViewHolder> selectedItems) {
                    _changeToolbarsItemSelected(selectedItems);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void _configureSearchBox() {
        conversationsViewModel.loadAll(getApplicationContext());

        TextInputLayout textInputLayout = findViewById(R.id.conversations_search_box_layout);
        textInputLayout.setVisibility(View.VISIBLE);

        findViewById(R.id.conversations_search_results_found).setVisibility(View.VISIBLE);

        TextInputEditText textInputEditText = findViewById(R.id.conversations_search_box);
        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable != null && editable.length() > 1)
                    searchForInput(editable.toString());
                else
                    searchPositions.setValue(new ArrayList<>());
            }
        });
    }

    private void _configureToolbars() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (R.id.copy == id) {
                    _copyItems();
                    return true;
                }
                else if (R.id.delete == id || R.id.delete_multiple == id) {
                    try {
                        _deleteItems();
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                else if (R.id.make_call == id) {
                    smsMetaEntity.call(getApplicationContext());
                    return true;
                }
                else if(R.id.search_conversations == id) {
                    _configureSearchBox();
                }
                return false;
            }
        });

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(smsMetaEntity.getContactName(getApplicationContext()));
    }

    private void _configureMessagesTextBox() throws GeneralSecurityException, IOException {
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
                _onLongClickSendButton(v);
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

        final boolean hasSecretKey = smsMetaEntity.getEncryptionState(getApplicationContext())
                == SMS.SMSMetaEntity.ENCRYPTION_STATE.ENCRYPTED;
        final byte[] secretKey = smsMetaEntity.getSecretKey(getApplicationContext());

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

                try {
                    if (!s.toString().isEmpty() && hasSecretKey) {
                        String encryptedString = Base64.encodeToString(
                                SecurityAES.encrypt_256_cbc(s.toString().getBytes(StandardCharsets.UTF_8),
                                        secretKey, null),
                                Base64.DEFAULT);

                        encryptedString = SecurityHelpers.putEncryptedMessageWaterMark(encryptedString);
                        String stats = SMSHandler.calculateSMS(encryptedString);
                        String displayedString = encryptedString + "\n\n" + stats;

                        encryptedMessageTextView.setVisibility(View.VISIBLE);
                        encryptedMessageTextView.setText(displayedString);
                        if (encryptedMessageTextView.getLayout() != null)
                            encryptedMessageTextView.scrollTo(0,
                                    encryptedMessageTextView.getLayout().getLineTop(
                                            encryptedMessageTextView.getLineCount()) - encryptedMessageTextView.getHeight());
                    } else {
                        encryptedMessageTextView.setVisibility(View.GONE);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        // Message has been shared from another app to send by SMS
        if (getIntent().hasExtra(SMS.SMSMetaEntity.SHARED_SMS_BODY)) {
            smsTextView.setText(getIntent().getStringExtra(SMS.SMSMetaEntity.SHARED_SMS_BODY));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mutableLiveDataComposeMessage
                            .setValue(getIntent().getStringExtra(SMS.SMSMetaEntity.SHARED_SMS_BODY));
                }
            });
        }
    }

    private void _configureLayoutForMessageType() {
        if(smsMetaEntity.isShortCode()) {
            // Cannot reply to message
            ConstraintLayout smsLayout = findViewById(R.id.send_message_content_layouts);
            smsLayout.setVisibility(View.GONE);
        }
    }

    private void _updateThreadToRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int updatedCount = SMSHandler.updateMarkThreadMessagesAsRead(getApplicationContext(),
                        smsMetaEntity.getThreadId());
            }
        }).start();
    }

    private void _changeToolbarsItemSelected(HashMap<String, RecyclerView.ViewHolder> selectedItems) {
        if (selectedItems != null) {
            if (selectedItems.isEmpty()) {
                showDefaultToolbar(toolbar.getMenu());
            } else {
                hideDefaultToolbar(toolbar.getMenu(), selectedItems.size());
            }
        }
    }

    private void _configureEncryptionListeners() {
        try {
            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showMultiSimcardAlert(Runnable runnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.sim_chooser_layout_text));
//        builder.setMessage(getString(R.string.messages_thread_delete_confirmation_text));

        View simChooserView = View.inflate(getApplicationContext(), R.layout.sim_chooser_layout, null);
        builder.setView(simChooserView);

        List<SubscriptionInfo> subscriptionInfos = SIMHandler.getSimCardInformation(getApplicationContext());

        Bitmap sim1Bitmap = subscriptionInfos.get(0).createIconBitmap(getApplicationContext());
        Bitmap sim2Bitmap = subscriptionInfos.get(1).createIconBitmap(getApplicationContext());

        ImageView sim1ImageView = simChooserView.findViewById(R.id.sim_layout_simcard_1_img);
        TextView sim1TextView = simChooserView.findViewById(R.id.sim_layout_simcard_1_name);

        ImageView sim2ImageView = simChooserView.findViewById(R.id.sim_layout_simcard_2_img);
        TextView sim2TextView = simChooserView.findViewById(R.id.sim_layout_simcard_2_name);

        sim1ImageView.setImageBitmap(sim1Bitmap);
        AlertDialog dialog = builder.create();

        sim1ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defaultSubscriptionId = subscriptionInfos.get(0).getSubscriptionId();
                runnable.run();
                dialog.dismiss();
            }
        });
        sim1TextView.setText(subscriptionInfos.get(0).getDisplayName());

        sim2ImageView.setImageBitmap(sim2Bitmap);
        sim2ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defaultSubscriptionId = subscriptionInfos.get(1).getSubscriptionId();
                runnable.run();
                dialog.dismiss();
            }
        });
        sim2TextView.setText(subscriptionInfos.get(1).getDisplayName());

        dialog.show();
    }

    private void _txAgreementKey() throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(getApplicationContext());
        try {
            KeyPair keyPair  = smsMetaEntity.generateAgreements(getApplicationContext());
            byte[] agreementKey = SecurityHelpers.txAgreementFormatter(
                    keyPair.getPublic().getEncoded());
            securityECDH.securelyStorePrivateKeyKeyPair(getApplicationContext(),
                    smsMetaEntity.getAddress(), keyPair);
            int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
            String threadId = SMSHandler.registerPendingKeyMessage(getApplicationContext(),
                    smsMetaEntity.getAddress(),
                    agreementKey,
                    subscriptionId);

            if(smsMetaEntity.getThreadId() == null && threadId != null) {
                getIntent().putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                _setupActivityDependencies();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void _checkEncryptionStatus() throws GeneralSecurityException, IOException {
        if(smsMetaEntity.isShortCode() ||
                SettingsHandler.alertNotEncryptedCommunicationDisabled(getApplicationContext())) {
            return;
        }

        Log.d(getLocalClassName(), "Encryption status: " + smsMetaEntity.getEncryptionState(getApplicationContext()));
        if(smsMetaEntity.getEncryptionState(getApplicationContext()) ==
                SMS.SMSMetaEntity.ENCRYPTION_STATE.NOT_ENCRYPTED) {
            ab.setSubtitle(R.string.send_sms_activity_user_not_encrypted);

            int textColor = Color.WHITE;
            Integer bgColor = getResources().getColor(R.color.failed_red, getTheme());
            String conversationNotSecuredText = getString(R.string.send_sms_activity_user_not_secure);
            String actionText = getString(R.string.send_sms_activity_user_not_secure_yes);

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(SIMHandler.getActiveSimcardCount(getApplicationContext()) > 1) {
                        showMultiSimcardAlert(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    _txAgreementKey();
                                } catch (GeneralSecurityException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else {
                        try {
                            _txAgreementKey();
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            lunchSnackBar(conversationNotSecuredText, actionText, onClickListener, bgColor, textColor);
        }
        else if(smsMetaEntity.getEncryptionState(getApplicationContext()) ==
                SMS.SMSMetaEntity.ENCRYPTION_STATE.SENT_PENDING_AGREEMENT) {
            ab.setSubtitle(R.string.send_sms_activity_user_not_encrypted);

            int bgColor = getResources().getColor(R.color.purple_200, getTheme());
            String conversationNotSecuredText = getString(R.string.send_sms_activity_user_not_secure_pending);
            String actionText = getString(R.string.send_sms_activity_user_not_secure_pending_yes);

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(SIMHandler.getActiveSimcardCount(getApplicationContext()) > 1) {
                        showMultiSimcardAlert(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    _txAgreementKey();
                                } catch (GeneralSecurityException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else {
                        try {
                            _txAgreementKey();
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            lunchSnackBar(conversationNotSecuredText, actionText, onClickListener, bgColor, Color.BLACK);
        }
        else if(smsMetaEntity.getEncryptionState(getApplicationContext()) ==
                SMS.SMSMetaEntity.ENCRYPTION_STATE.RECEIVED_PENDING_AGREEMENT) {
            String text = getString(R.string.send_sms_activity_user_not_secure_agree);
            String actionText = getString(R.string.send_sms_activity_user_not_secure_yes_agree);
            Integer bgColor = getResources().getColor(R.color.highlight_yellow, getTheme());
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        smsMetaEntity.agreePeerRequest(getApplicationContext());
                    } catch (GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            lunchSnackBar(text, actionText, onClickListener, bgColor, Color.BLACK);
        }
        else if(smsMetaEntity.getEncryptionState(getApplicationContext()) ==
                SMS.SMSMetaEntity.ENCRYPTION_STATE.RECEIVED_AGREEMENT_REQUEST) {
            String text = getString(R.string.send_sms_activity_user_not_secure_no_agreed);
            String actionText = getString(R.string.send_sms_activity_user_not_secure_yes_agree);
            int bgColor = getResources().getColor(R.color.purple_200, getTheme());

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        byte[] agreementKey = smsMetaEntity.agreePeerRequest(getApplicationContext());

                        // TODO: refactor the entire send sms thing to inform when dual-sim
                        // TODO: support for multi-sim
                        int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
                        String threadId = SMSHandler.registerPendingKeyMessage(getApplicationContext(),
                                smsMetaEntity.getAddress(),
                                agreementKey,
                                subscriptionId);

                        if(smsMetaEntity.getThreadId() == null && threadId != null) {
                            getIntent().putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                            _setupActivityDependencies();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            lunchSnackBar(text, actionText, onClickListener, bgColor, Color.BLACK);
        }
        else if(smsMetaEntity.getEncryptionState(getApplicationContext()) ==
                SMS.SMSMetaEntity.ENCRYPTION_STATE.ENCRYPTED) {
            ab.setSubtitle(getString(R.string.send_sms_activity_user_encrypted));
        }
    }

    /**
     *  Checks if encryption key is available, if available tries to encrypt the data.
     *  In case of any key damage or cannot encrypt, returns the original data.
     * @param data
     * @return
     * @throws Throwable
     */
    private String _encryptContent(String data, byte[] secretKey) {
        try {
            byte[] encryptedContent = SecurityAES.encrypt_256_cbc(data.getBytes(StandardCharsets.UTF_8),
                    secretKey, null);
            data = Base64.encodeToString(encryptedContent, Base64.DEFAULT);
        } catch(Throwable e ) {
            e.printStackTrace();
        }
        return data;
    }

    public void sendTextMessage(View view) throws Exception {
        if(smsTextView.getText() != null) {
            String text = smsTextView.getText().toString();

            if(smsMetaEntity.hasSecretKey(getApplicationContext())) {
                text = _encryptContent(text, smsMetaEntity.getSecretKey(getApplicationContext()));
                text = SecurityHelpers.putEncryptedMessageWaterMark(text);
            }

            String threadId = _sendSMSMessage(defaultSubscriptionId, text);
            Log.d(getLocalClassName(), "Sending sms with thread: " + threadId + ":" + smsMetaEntity.getThreadId());
            if(smsMetaEntity.getThreadId() == null && threadId != null) {
                getIntent().putExtra(SMS.SMSMetaEntity.THREAD_ID, threadId);
                _setupActivityDependencies();
            }

            // Remove messages from archive if pending send
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        removeFromArchive();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            smsTextView.getText().clear();
        }
    }

    private void removeFromArchive() throws InterruptedException {
        ArchiveHandler archiveHandler = new ArchiveHandler(getApplicationContext());
        archiveHandler.removeFromArchive(getApplicationContext(),
                Long.parseLong(smsMetaEntity.getThreadId()));
    }

    private String _sendSMSMessage(int subscriptionId, String text) {
        String threadId = new String();
        try {
            threadId = SMSHandler.registerPendingMessage(getApplicationContext(),
                    smsMetaEntity.getAddress(), text, subscriptionId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return threadId;
    }

    private String _sendKeyDataMessage(int subscriptionId, byte[] data) {
        String threadId = new String();
        try {
            threadId = SMSHandler.registerPendingKeyMessage(getApplicationContext(),
                    smsMetaEntity.getAddress(), data, subscriptionId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return threadId;
    }

    private void lunchSnackBar(String text, String actionText, View.OnClickListener onClickListener,
                               Integer bgColor, Integer textColor) {
        String insertDetails = smsMetaEntity.getContactName(getApplicationContext());
        insertDetails = insertDetails.replaceAll("\\+", "");
        String insertText = text.replaceAll("\\[insert name\\]", insertDetails);

        SpannableStringBuilder spannable = new SpannableStringBuilder(insertText);

        Pattern pattern = Pattern.compile(insertDetails); // Regex pattern to match "[phonenumber]"
        Matcher matcher = pattern.matcher(spannable);

        while (matcher.find()) {
            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            spannable.setSpan(boldSpan, matcher.start(), matcher.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator),
                spannable, BaseTransientBottomBar.LENGTH_INDEFINITE);

        View snackbarView = snackbar.getView();

        snackbar.setTextColor(textColor);

        if (bgColor == null)
            bgColor = getResources().getColor(R.color.primary_warning_background_color,
                    getTheme());

        snackbar.setBackgroundTint(bgColor);
        snackbar.setTextMaxLines(10);
        snackbar.setActionTextColor(textColor);
        snackbar.setAction(actionText, onClickListener);
        snackbar.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }

    public void uploadImage(View view) {
//        Intent galleryIntent = new Intent(
//                Intent.ACTION_PICK,
//                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//
//        startActivityForResult(galleryIntent, RESULT_GALLERY);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == RESULT_GALLERY) {
//            if (null != data) {
//                Uri imageUri = data.getData();
//
//                Intent intent = new Intent(getApplicationContext(), ImageViewActivity.class);
//                intent.putExtra(IMAGE_URI, imageUri.toString());
//                intent.putExtra(ADDRESS, address);
//                intent.putExtra(THREAD_ID, threadId);
//                startActivity(intent);
//                finish();
//            }
//        }
    }

    public void _onLongClickSendButton(View view) {
//        List<SubscriptionInfo> simcards = SIMHandler.getSimCardInformation(getApplicationContext());
//
//        TextView simcard1 = findViewById(R.id.simcard_select_operator_1_name);
//        TextView simcard2 = findViewById(R.id.simcard_select_operator_2_name);
//
//        ImageButton simcard1Img = findViewById(R.id.simcard_select_operator_1);
//        ImageButton simcard2Img = findViewById(R.id.simcard_select_operator_2);
//
//        ArrayList<TextView> views = new ArrayList();
//        views.add(simcard1);
//        views.add(simcard2);
//
//        ArrayList<ImageButton> buttons = new ArrayList();
//        buttons.add(simcard1Img);
//        buttons.add(simcard2Img);
//
//        for (int i = 0; i < simcards.size(); ++i) {
//            CharSequence carrierName = simcards.get(i).getCarrierName();
//            views.get(i).setText(carrierName);
//            buttons.get(i).setImageBitmap(simcards.get(i).createIconBitmap(getApplicationContext()));
//
//            final int subscriptionId = simcards.get(i).getSubscriptionId();
//            buttons.get(i).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    defaultSubscriptionId = subscriptionId;
//                    findViewById(R.id.simcard_select_constraint).setVisibility(View.INVISIBLE);
//                    String subscriptionText = getString(R.string.default_subscription_id_changed) +
//                            carrierName;
//                    Toast.makeText(getApplicationContext(), subscriptionText, Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        multiSimcardConstraint.setVisibility(View.VISIBLE);
    }

    private void hideDefaultToolbar(Menu menu, int size) {
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
        ab.setElevation(10);
    }

    private void showDefaultToolbar(Menu menu) {
        if(!smsMetaEntity.isShortCode())
            menu.setGroupVisible(R.id.default_menu_items, true);
//            menu.setGroupVisible(R.id.default_menu_items, false);
        menu.setGroupVisible(R.id.single_message_copy_menu, false);

        ab.setHomeAsUpIndicator(null);
        ab.setTitle(smsMetaEntity.getContactName(getApplicationContext()));
        try {
            _checkEncryptionStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void _copyItems() {
        if(conversationsRecyclerAdapter.selectedItem.getValue() != null) {
            String[] keys = conversationsRecyclerAdapter.selectedItem.getValue()
                    .keySet().toArray(new String[0]);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            Cursor cursor = SMSHandler.fetchSMSInboxById(getApplicationContext(), keys[0]);
            if (cursor.moveToFirst()) {
                do {
                    SMS sms = new SMS(cursor);
                    ClipData clip = ClipData.newPlainText(keys[0], sms.getBody());

                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Copied!", Toast.LENGTH_SHORT).show();

                } while (cursor.moveToNext());
            }
            cursor.close();
            conversationsRecyclerAdapter.resetSelectedItem(keys[0], true);
        }
    }

    private void _deleteItems() throws Exception {
        if(conversationsRecyclerAdapter.selectedItem.getValue() != null) {
            final String[] keys = conversationsRecyclerAdapter.selectedItem.getValue()
                    .keySet().toArray(new String[0]);
            if (keys.length > 1) {
                smsMetaEntity.deleteMultipleMessages(getApplicationContext(), keys);
                conversationsRecyclerAdapter.resetAllSelectedItems();
                conversationsRecyclerAdapter.removeAllItems(keys);
            } else {
                smsMetaEntity.deleteMessage(getApplicationContext(), keys[0]);
                conversationsRecyclerAdapter.resetSelectedItem(keys[0], true);
                conversationsRecyclerAdapter.removeItem(keys[0]);
            }
        }
    }

    private void searchForInput(String search){
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversations> searchMessages = new ArrayList<>();

                Cursor cursorSearch = SMSHandler.fetchSMSMessagesForSearch(getApplicationContext(), search);
                Cursor cursorAll = smsMetaEntity.fetchMessages(getApplicationContext(), 0, 0);

                if(cursorSearch.moveToFirst()) {
                    do {
                        int threadIndex = cursorSearch.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID);
                        int messageIdIndex = cursorSearch.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);

                        String threadId = String.valueOf(cursorSearch.getString(threadIndex));
                        String messageId = String.valueOf(cursorSearch.getString(messageIdIndex));

                        Conversations conversations = new Conversations();
                        conversations.setTHREAD_ID(threadId);
                        conversations.setMESSAGE_ID(messageId);

                        searchMessages.add(conversations);
                    } while(cursorSearch.moveToNext());
                    cursorSearch.close();
                }

                List<Integer> foundPositions = new ArrayList<>();
                if(cursorAll.moveToFirst()) {
                    int position = 0;
                    do {
                        int threadIndex = cursorAll.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID);
                        int messageIdIndex = cursorAll.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);

                        String threadId = cursorAll.getString(threadIndex);
                        String messageId = cursorAll.getString(messageIdIndex);

                        Conversations conversations = new Conversations();
                        conversations.setTHREAD_ID(threadId);
                        conversations.setMESSAGE_ID(messageId);

                        if(searchMessages.contains(conversations)) {
                            foundPositions.add(position);
                        }

                        ++position;
                    } while(cursorAll.moveToNext());
                    cursorAll.close();
                }
                searchPositions.postValue(foundPositions);
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SEND_SMS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    Toast.makeText(this, "Let's do this!!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}