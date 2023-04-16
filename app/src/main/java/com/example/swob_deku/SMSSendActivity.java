package com.example.swob_deku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Archive.Archive;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.Messages.SingleMessageViewModel;
import com.example.swob_deku.Models.Messages.SingleMessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityAES;
import com.example.swob_deku.Models.Security.SecurityDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;

//import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSSendActivity extends AppCompatActivity {
    SingleMessagesThreadRecyclerAdapter singleMessagesThreadRecyclerAdapter;
    SingleMessageViewModel singleMessageViewModel;
    TextInputEditText smsTextView;
    TextInputLayout smsTextInputLayout;

    ConstraintLayout multiSimcardConstraint;

    MutableLiveData<String> mutableLiveDataComposeMessage = new MutableLiveData<>();

    public static final String COMPRESSED_IMAGE_BYTES = "COMPRESSED_IMAGE_BYTES";
    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String ADDRESS = "address";
    public static final String THREAD_ID = "thread_id";
    public static final String ID = "_id";
    public static final String SEARCH_STRING = "search_string";
    public static final String SEARCH_OFFSET = "search_offset";

    public static final String SMS_SENT_INTENT = "SMS_SENT";
    public static final String SMS_DELIVERED_INTENT = "SMS_DELIVERED";

    public static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 200;
    private final int RESULT_GALLERY = 100;

    String threadId = "";
    String address = "";

    String contactName = "";

    Toolbar toolbar;
    ActionBar ab;

    LinearLayoutManager linearLayoutManager;
    RecyclerView singleMessagesThreadRecyclerView;

    BroadcastReceiver incomingDataBroadcastReceiver;
    BroadcastReceiver incomingBroadcastReceiver;

    private void configureToolbars() {
        toolbar = (Toolbar) findViewById(R.id.send_smsactivity_toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        enableToolbar();
    }

    HashMap<String, RecyclerView.ViewHolder> selectedItems = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_smsactivity);

        if(!checkIsDefaultApp()) {
            startActivity(new Intent(this, DefaultCheckActivity.class));
            finish();
            return;
        }

        smsTextView = findViewById(R.id.sms_text);
        threadIdentificationLoader();
        try {
            singleMessagesThreadRecyclerAdapter = new SingleMessagesThreadRecyclerAdapter(
                    getApplicationContext(), address);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
        multiSimcardConstraint = findViewById(R.id.simcard_select_constraint);

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(false);
        linearLayoutManager.setReverseLayout(true);

        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view);
        singleMessagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
        singleMessagesThreadRecyclerView.setAdapter(singleMessagesThreadRecyclerAdapter);

        singleMessageViewModel = new ViewModelProvider( this)
                .get( SingleMessageViewModel.class);

        configureToolbars();

        int offset = getIntent().getIntExtra(SEARCH_OFFSET, 0);
        singleMessageViewModel.getMessages(
                getApplicationContext(), threadId, offset).observe(this, new Observer<List<SMS>>() {
            @Override
            public void onChanged(List<SMS> smsList) {
                Log.d(getLocalClassName(), "Paging data changed!");
                singleMessagesThreadRecyclerAdapter.mDiffer.submitList(smsList);
            }
        });

        eventListeners();
    }

    private boolean checkIsDefaultApp() {
        final String myPackageName = getPackageName();
        final String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);

        return myPackageName.equals(defaultPackage);
    }

    private void threadIdentificationLoader() {
        if(threadId.isEmpty()) {
            try {
                getAddressAndThreadId();
            } catch (InterruptedException | NumberParseException e) {
                throw new RuntimeException(e);
            }
        }
        Log.d(getLocalClassName(), "Fetching view model starting");
    }

    private void eventListeners() {
        if(mutableLiveDataComposeMessage.getValue() == null || mutableLiveDataComposeMessage.getValue().isEmpty())
            findViewById(R.id.sms_send_button).setVisibility(View.INVISIBLE);

        mutableLiveDataComposeMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                findViewById(R.id.sms_send_button).setVisibility(s.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            }
        });

        singleMessagesThreadRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                final int lastVisiblePos = ((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findLastVisibleItemPosition();

//                if(lastVisiblePos >= recyclerView.getAdapter().getItemCount() - 1) {
                final int scrollPosition = singleMessagesThreadRecyclerAdapter.getItemCount() - 1;
                Log.d(getLocalClassName(), "Last visible position: " + lastVisiblePos);
                Log.d(getLocalClassName(), "Scroll position: " + scrollPosition);
                if(lastVisiblePos >= scrollPosition) {
                    singleMessageViewModel.refresh();
                    int itemCount = recyclerView.getAdapter().getItemCount();
                    if(itemCount > scrollPosition + 1)
                        recyclerView.scrollToPosition(lastVisiblePos);
                }
                else if(!singleMessageViewModel.offsetStartedFromZero &&
                        lastVisiblePos >= (scrollPosition/2)){
                    Log.d(getLocalClassName(), "Should scroll downwards..");
                    singleMessageViewModel.refreshDown();
//                    int itemCount = recyclerView.getAdapter().getItemCount();
//                    if(itemCount > scrollPosition - 1)
//                        recyclerView.scrollToPosition(lastVisiblePos);
                }
            }
        });

        findViewById(R.id.sms_send_button).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickSend(v);
                return true;
            }
        });

        multiSimcardConstraint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getVisibility() == View.VISIBLE)
                    v.setVisibility(View.INVISIBLE);
            }
        });

        singleMessagesThreadRecyclerAdapter.selectedItem.observe(this, new Observer<HashMap<String, RecyclerView.ViewHolder>>() {
            @Override
            public void onChanged(HashMap<String, RecyclerView.ViewHolder> integers) {
                selectedItems = integers;
                itemOperationsNeeded();
            }
        });
    }

    private void getAddressAndThreadId() throws InterruptedException, NumberParseException {
        processForSharedIntent();
        getMessagesThreadId();
    }

    private void updateMessagesToRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
               int updatedCount = SMSHandler.updateThreadMessagesThread(getApplicationContext(), threadId);
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Updating read for threadID: " + threadId + "->"+ updatedCount);
            }
        }).start();
    }

    private void getMessagesThreadId() throws NumberParseException {
        if(getIntent().hasExtra(THREAD_ID)) {
            threadId = getIntent().getStringExtra(THREAD_ID);
            Cursor cursor = SMSHandler.fetchSMSAddressFromThreadId(getApplicationContext(), threadId);

            if(cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
                address = String.valueOf(cursor.getString(addressIndex));
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Found Address: " + address);
            }

            cursor.close();
        }

        else if(getIntent().hasExtra(ADDRESS) || !address.isEmpty()) {
            if(address.isEmpty())
                address = getIntent().getStringExtra(ADDRESS);

            if(BuildConfig.DEBUG)
                Log.d(getLocalClassName(), "Searching thread ID with address: " + address);

            Cursor cursor = SMSHandler.fetchSMSThreadIdFromAddress(getApplicationContext(), address);
            if(cursor.moveToFirst()) {
                int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
                threadId = String.valueOf(cursor.getString(threadIdIndex));

                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Found thread ID: " + threadId);
            }
            cursor.close();
        }

        try {
            address = Helpers.formatPhoneNumbers(address);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void improveMessagingUX() {
        smsTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                view.getParent().requestDisallowInterceptTouchEvent(true);
                if ((motionEvent.getAction() & MotionEvent.ACTION_UP) != 0 &&
                        (motionEvent.getActionMasked() & MotionEvent.ACTION_UP) != 0)
                {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!PhoneNumberUtils.isWellFormedSmsAddress(address)) {
                        ConstraintLayout smsLayout = findViewById(R.id.send_message_content_layouts);
                        smsLayout.setVisibility(View.GONE);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        contactName = Contacts.retrieveContactName(getApplicationContext(), address);
        contactName = (contactName.equals("null") || contactName.isEmpty()) ?
                address: contactName;
    }

    private void processForSharedIntent() throws NumberParseException {
//        String indentAction = getIntent().getAction();
//
//        if(BuildConfig.DEBUG)
//            Log.d(getLocalClassName(), "Processing shared..." + indentAction);

        if(getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SENDTO) ){
            String sendToString = getIntent().getDataString();

            if(BuildConfig.DEBUG)
                Log.d("", "Processing shared #: " + sendToString);

//            sendToString = sendToString.replace("%2B", "+")
//                            .replace("%20", "");
//            sendToString = Helpers.formatPhoneNumbers(sendToString);

            if(sendToString.contains("smsto:") || sendToString.contains("sms:")) {
               address = sendToString.substring(sendToString.indexOf(':') + 1);
               address = Helpers.formatPhoneNumbers(address);
               String text = getIntent().getStringExtra("sms_body");
                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Processing shared body: " + text);

               // TODO: should inform view about data being available
//               if(getIntent().hasExtra(Intent.EXTRA_INTENT)) {
//                   byte[] bytesData = getIntent().getByteArrayExtra(Intent.EXTRA_STREAM);
//                   if (bytesData != null) {
//                       Log.d(getClass().getName(), "Byte data: " + bytesData);
//                       Log.d(getClass().getName(), "Byte data: " + new String(bytesData, StandardCharsets.UTF_8));
//
//                       text = new String(bytesData, StandardCharsets.UTF_8);
//                       getIntent().putExtra(Intent.EXTRA_INTENT, getIntent().getByteArrayExtra(Intent.EXTRA_INTENT));
//                   }
//               }

               if(text != null && !text.isEmpty()) {
                   smsTextView.setText(text);

                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           mutableLiveDataComposeMessage.setValue(text);
                       }
                   });
               }
            }
        }
    }

    public void handleIncomingBroadcast() {
        incomingBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                if(singleMessageViewModel.getLastUsedKey() == 0)
//                    singleMessagesThreadRecyclerAdapter.refresh();
                singleMessageViewModel.informNewItemChanges();
                cancelNotifications(getIntent().getStringExtra(THREAD_ID));
            }
        };

        incomingDataBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                singleMessageViewModel.informNewItemChanges();
//                cancelNotifications(getIntent().getStringExtra(THREAD_ID));
            }
        };

        // SMS_RECEIVED = global broadcast informing all apps listening a message has arrived
        registerReceiver(incomingBroadcastReceiver,
                new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        registerReceiver(incomingDataBroadcastReceiver,
                new IntentFilter(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION));
        registerReceiver(incomingDataBroadcastReceiver,
                new IntentFilter(BuildConfig.APPLICATION_ID + ".DATA_SMS_RECEIVED_ACTION"));
    }

    public void handleBroadcast() {
//        https://developer.android.com/reference/android/telephony/SmsManager.html#sendTextMessage(java.lang.String,%20java.lang.String,%20java.lang.String,%20android.app.PendingIntent,%20android.app.PendingIntent,%20long)

        BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                long id = intent.getLongExtra(ID, -1);

                if(BuildConfig.DEBUG)
                    Log.d(getLocalClassName(), "Broadcast received for sent: " + id);

                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        try {
                            SMSHandler.registerSentMessage(getApplicationContext(), id);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    default:
                        try {
                            SMSHandler.registerFailedMessage(context, id, getResultCode());
                        } catch(Exception e) {
                            e.printStackTrace();
                        }

                        if(BuildConfig.DEBUG) {
                            Log.d(getLocalClassName(), "Failed to send: " + getResultCode());
                            Log.d(getLocalClassName(), "Failed to send: " + getResultData());
                            Log.d(getLocalClassName(), "Failed to send: " + intent.getData());
                        }
                }

//                if(singleMessageViewModel.getLastUsedKey() == 0)
//                    singleMessagesThreadRecyclerAdapter.refresh();

                singleMessageViewModel.informNewItemChanges();
                unregisterReceiver(this);
            }
        };

        BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(ID, -1);

                if (getResultCode() == Activity.RESULT_OK) {
                    SMSHandler.registerDeliveredMessage(context, id);
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(getLocalClassName(), "Failed to deliver: " + getResultCode());
                }

//                if(singleMessageViewModel.getLastUsedKey() == 0)
//                    singleMessagesThreadRecyclerAdapter.refresh();

                singleMessageViewModel.informNewItemChanges();
                unregisterReceiver(this);
            }
        };

        registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_INTENT));
        registerReceiver(sentBroadcastReceiver, new IntentFilter(SMS_SENT_INTENT));

    }

    public void cancelNotifications(String threadId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(
                getApplicationContext());

        if(getIntent().hasExtra(THREAD_ID))
            notificationManager.cancel(Integer.parseInt(threadId));
    }

    public static PendingIntent[] getPendingIntents(Context context, long messageId) {
        Intent sentIntent = new Intent(SMS_SENT_INTENT);
        sentIntent.putExtra(SMSSendActivity.ID, messageId);

        Intent deliveredIntent = new Intent(SMS_DELIVERED_INTENT);
        deliveredIntent.putExtra(SMSSendActivity.ID, messageId);

        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)), sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                Integer.parseInt(String.valueOf(messageId)), deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
    }

    public void sendTextMessage(View view) {
        String text = smsTextView.getText().toString();
        sendSMSMessage(null, text, null);
    }

    private String encryptContent(String data) throws Throwable {
        SecurityDH securityDH = new SecurityDH(getApplicationContext());
        if(securityDH.hasSecretKey(address)) {
            byte[] secretKey = Base64.decode(securityDH.securelyFetchSecretKey(address), Base64.DEFAULT);
            // TODO: begin encrypting data
            // TODO: if can't encrypt data return original data

            try {
                byte[] encryptedContent = SecurityDH.encryptAES(data.getBytes(StandardCharsets.UTF_8),
                        secretKey);
                data = Base64.encodeToString(encryptedContent, Base64.DEFAULT);
            } catch(Exception e ) {
                e.printStackTrace();
            }
        }
        return data;
    }


    private void sendSMSMessage(Integer subscriptionId, String text, Long messageId) {
        // TODO: Don't let sending happen if message box is empty
        Log.d(getLocalClassName(), "Sending new text message..");
        try {

            SecurityDH securityDH = new SecurityDH(getApplicationContext());

            if(messageId == null)
                messageId = Helpers.generateRandomNumber();

            PendingIntent[] pendingIntents = getPendingIntents(getApplicationContext(), messageId);

            handleBroadcast();

            String tmpThreadId = null;
            if(securityDH.hasSecretKey(address)) {
                text = encryptContent(text);
                text = SecurityHelpers.waterMarkMessage(text);
                tmpThreadId = SMSHandler.sendEncryptedTextSMS(getApplicationContext(), address, text,
                        pendingIntents[0], pendingIntents[1], messageId, subscriptionId);
            }
            else tmpThreadId = SMSHandler.sendTextSMS(getApplicationContext(), address, text,
                    pendingIntents[0], pendingIntents[1], messageId, subscriptionId);

            resetSmsTextView();

            if(threadId == null || threadId.isEmpty()) {
                threadId = tmpThreadId;
                singleMessageViewModel.informNewItemChanges(threadId);
            }
            else {
                singleMessageViewModel.informNewItemChanges();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        removeFromArchive();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        catch(IllegalArgumentException e ) {
            e.printStackTrace();
            Toast.makeText(this, "Make sure Address and Text are provided.", Toast.LENGTH_LONG).show();
        }
        catch(Throwable e ) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong, check log stack", Toast.LENGTH_LONG).show();
        }
    }

    private void removeFromArchive() throws InterruptedException {
        ArchiveHandler.removeFromArchive(getApplicationContext(), Long.parseLong(threadId));
    }

    private void resetSmsTextView() {
//        smsTextView.setText(null);
        smsTextView.getText().clear();
    }

    public boolean checkPermissionToSendSMSMessages() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
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

    @Override
    protected void onResume() {
        super.onResume();
        handleIncomingBroadcast();

        try {
            getAddressAndThreadId();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NumberParseException e) {
            e.printStackTrace();
        }

        improveMessagingUX();
        ab.setTitle(contactName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                updateMessagesToRead();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (PhoneNumberUtils.isWellFormedSmsAddress(address)) {
                        checkEncryptedMessaging();
                        if(getIntent().hasExtra(ImageViewActivity.SMS_IMAGE_PENDING_LOCATION)) {
                            long messageId = getIntent().getLongExtra(ImageViewActivity.SMS_IMAGE_PENDING_LOCATION, -1);
                            handleIncomingPending(messageId);
                        }
                    }
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleIncomingPending(long messageId) {
        Log.d(getLocalClassName(), "Component package name: " + getIntent().getComponent().getPackageName());
        Log.d(getLocalClassName(), "My package name: " + BuildConfig.APPLICATION_ID);
        if(getIntent().getComponent().getPackageName().equals(BuildConfig.APPLICATION_ID) ) {
            Cursor cursor = SMSHandler.fetchSMSOutboxPendingForMessageInThread(getApplicationContext(),
                    threadId, messageId);
            Log.d(getLocalClassName(), "Found pending: " + cursor.getCount());

            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendSMSMessage(null, sms.getBody(), Long.parseLong(sms.getId()));
                    }
                });
            }
            cursor.close();
        }
    }

    private void lunchSnackBar(String text, String actionText, View.OnClickListener onClickListener, Integer bgColor) {
        String insertDetails = contactName.isEmpty() ? address : contactName;
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
//            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbarView;
//            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//            View customView = inflater.inflate(R.layout.layout_security_snackbar, this.);
//            snackbarLayout.addView(customView, 0);

        snackbar.setTextColor(getResources().getColor(R.color.default_gray, getTheme()));

        if(bgColor == null)
            bgColor = getResources().getColor(R.color.primary_warning_background_color,
                    getTheme());

        snackbar.setBackgroundTint(bgColor);
        snackbar.setTextMaxLines(4);
        snackbar.setActionTextColor(getResources().getColor(R.color.white, getTheme()));
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

    private void rxKeys(byte[][] txAgreementKey, long messageId, int subscriptionId){
        try {
            PendingIntent[] pendingIntents = getPendingIntents(getApplicationContext(), messageId);

            handleBroadcast();
            SMSHandler.sendDataSMS(getApplicationContext(),
                    address,
                    txAgreementKey[0],
                    pendingIntents[0],
                    pendingIntents[1],
                    messageId,
                    subscriptionId);

            handleBroadcast();
            SMSHandler.sendDataSMS(getApplicationContext(),
                    address,
                    txAgreementKey[1],
                    pendingIntents[0],
                    pendingIntents[1],
                    messageId,
                    subscriptionId);

        } catch (InterruptedException e) {
            e.printStackTrace();
            SMSHandler.registerFailedMessage(getApplicationContext(), messageId,
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        }
    }

    public void checkEncryptedMessaging() throws GeneralSecurityException, IOException {
        SecurityDH securityDH = new SecurityDH(getApplicationContext());
        Log.d(getLocalClassName(), "Has private key: " + securityDH.hasPrivateKey(address));
        Log.d(getLocalClassName(), "Has private key for address: " + address);

        if(securityDH.peerAgreementPublicKeysAvailable(getApplicationContext(), address)) {
            String text = getString(R.string.send_sms_activity_user_not_secure_no_agreed);
            String actionText = getString(R.string.send_sms_activity_user_not_secure_yes_agree);

            // TODO: change bgColor to match the intended use
            Integer bgColor = getResources().getColor(R.color.purple_200, getTheme());
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        byte[] peerPublicKey = Base64.decode(securityDH.getPeerAgreementPublicKey(address),
                                Base64.DEFAULT);
                        KeyPair keyPair = securityDH.generateKeyPairFromPublicKey(peerPublicKey);

                        Thread remotePeerHandshake = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                PublicKey publicKey = keyPair.getPublic();
                                byte[][] txAgreementKey = SecurityHelpers.txAgreementFormatter(publicKey.getEncoded());

                                String agreementText = SecurityHelpers.FIRST_HEADER
                                        + Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT)
                                        + SecurityHelpers.END_HEADER;
                                long messageId = Helpers.generateRandomNumber();
                                int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
                                String threadIdRx = SMSHandler.registerPendingMessage(getApplicationContext(),
                                        address, agreementText, messageId, subscriptionId);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (threadId.isEmpty()) {
                                            threadId = threadIdRx;
                                            singleMessageViewModel.informNewItemChanges(threadId);
                                        } else singleMessageViewModel.informNewItemChanges();
                                    }
                                });
                                try {
                                    securityDH.securelyStorePrivateKeyKeyPair(getApplicationContext(),
                                            address, keyPair);
                                } catch (GeneralSecurityException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                                rxKeys(txAgreementKey, messageId, subscriptionId);

                            }
                        });
                        try {
                            if(!securityDH.hasPrivateKey(address)) {
                                // TODO: support for multi-sim
                                remotePeerHandshake.start();
                                remotePeerHandshake.join();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Log.d(getLocalClassName(), "Agreement value for secret: " +
                                Base64.encodeToString(peerPublicKey, Base64.DEFAULT));
                        byte[] secret = securityDH.generateSecretKey(peerPublicKey, address);
                        securityDH.securelyStoreSecretKey(address, secret);
                        ab.setSubtitle(getString(R.string.send_sms_activity_user_encrypted));

                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };


            // TODO: check if has private key
            lunchSnackBar(text, actionText, onClickListener, bgColor);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ab.setSubtitle(R.string.send_sms_activity_user_not_encrypted);
                }
            });
        }
        else if(!securityDH.hasEncryption(address)) {

            String conversationNotSecuredText = getString(R.string.send_sms_activity_user_not_secure);

            String actionText = getString(R.string.send_sms_activity_user_not_secure_yes);

            View.OnClickListener onClickListener =  new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: generate the key
                    // TODO: register the key as 1 message with data header - hold on to ID in case failure
                    // TODO: send the key as 2 data messages
                    try {
                        byte[] agreementKey = dhAgreementInitiation();
                        byte[][] txAgreementKey = SecurityHelpers.txAgreementFormatter(agreementKey);

                        String text = SecurityHelpers.FIRST_HEADER
                                + Base64.encodeToString(agreementKey, Base64.DEFAULT)
                                + SecurityHelpers.END_HEADER;

                        long messageId = Helpers.generateRandomNumber();

                        // TODO: support for multi-sim
                        int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
                        String threadIdRx = SMSHandler.registerPendingMessage(getApplicationContext(),
                                address, text, messageId, subscriptionId);
                        if(threadId.isEmpty()) {
                                           threadId = threadIdRx;
                                           singleMessageViewModel.informNewItemChanges(threadId);
                                           } else singleMessageViewModel.informNewItemChanges();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxKeys(txAgreementKey, messageId, subscriptionId);
                                } catch(Exception e) {
                                                 e.printStackTrace();
                                                 }
                            }
                        }).start();

                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            lunchSnackBar(conversationNotSecuredText, actionText, onClickListener, null);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ab.setSubtitle(R.string.send_sms_activity_user_not_encrypted);
                }
            });
        }
        else {
            ab.setSubtitle(getString(R.string.send_sms_activity_user_encrypted));
        }
    }

    @Override
    public void onBackPressed() {
        if(findViewById(R.id.simcard_select_constraint).getVisibility() == View.VISIBLE)
            findViewById(R.id.simcard_select_constraint).setVisibility(View.INVISIBLE);
        if(singleMessagesThreadRecyclerAdapter.hasSelectedItems()) {
            singleMessagesThreadRecyclerAdapter.resetAllSelectedItems();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.single_messages_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && singleMessagesThreadRecyclerAdapter.hasSelectedItems()) {
            singleMessagesThreadRecyclerAdapter.resetAllSelectedItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void uploadImage(View view) {
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent , RESULT_GALLERY );
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_GALLERY) {
            if (null != data) {
                Uri imageUri = data.getData();

                Intent intent = new Intent(getApplicationContext(), ImageViewActivity.class);
                intent.putExtra(IMAGE_URI, imageUri.toString());
                intent.putExtra(ADDRESS, address);
                intent.putExtra(THREAD_ID, threadId);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(incomingBroadcastReceiver != null)
            unregisterReceiver(incomingBroadcastReceiver);

        if(incomingDataBroadcastReceiver != null)
            unregisterReceiver(incomingDataBroadcastReceiver);
    }

    public void onLongClickSend(View view) {
        List<SubscriptionInfo> simcards = SIMHandler.getSimCardInformation(getApplicationContext());

        TextView simcard1 = findViewById(R.id.simcard_select_operator_1_name);
        TextView simcard2 = findViewById(R.id.simcard_select_operator_2_name);

        ImageButton simcard1Img = findViewById(R.id.simcard_select_operator_1);
        ImageButton simcard2Img = findViewById(R.id.simcard_select_operator_2);

        ArrayList<TextView> views = new ArrayList();
        views.add(simcard1);
        views.add(simcard2);

        ArrayList<ImageButton> buttons = new ArrayList();
        buttons.add(simcard1Img);
        buttons.add(simcard2Img);

        for(int i=0;i<simcards.size(); ++i) {
            CharSequence carrierName = simcards.get(i).getCarrierName();
            views.get(i).setText(carrierName);
            buttons.get(i).setImageBitmap(simcards.get(i).createIconBitmap(getApplicationContext()));

            final int subscriptionId = simcards.get(i).getSubscriptionId();
            buttons.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = smsTextView.getText().toString();
                    sendSMSMessage(subscriptionId, text, null);
                    findViewById(R.id.simcard_select_constraint).setVisibility(View.INVISIBLE);
                }
            });
        }

        multiSimcardConstraint.setVisibility(View.VISIBLE);
    }

    private void hideDefaultToolbar(Menu menu) {
        menu.setGroupVisible(R.id.default_menu_items, false);
        menu.setGroupVisible(R.id.single_message_copy_menu, true);

        ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);

        // experimental
        ab.setElevation(10);
    }

    private void showDefaultToolbar(Menu menu) {
        menu.setGroupVisible(R.id.default_menu_items, true);
        menu.setGroupVisible(R.id.single_message_copy_menu, false);

        ab.setHomeAsUpIndicator(null);
    }

    private void copyItems() {
        String[] keys = selectedItems.keySet().toArray(new String[0]);
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
        singleMessagesThreadRecyclerAdapter.resetSelectedItem(keys[0]);
    }

    private void deleteItems() {
        String[] keys = selectedItems.keySet().toArray(new String[0]);
        SMSHandler.deleteMessage(getApplicationContext(), keys[0]);
        singleMessagesThreadRecyclerAdapter.resetSelectedItem(keys[0]);
        //                        singleMessageViewModel.informNewItemChanges();
        singleMessagesThreadRecyclerAdapter.removeItem(keys[0]);
    }

    private void makeCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + address));

        startActivity(callIntent);
    }

    private void itemOperationsNeeded() {
        if(selectedItems != null) {
            if (selectedItems.isEmpty()) {
                showDefaultToolbar(toolbar.getMenu());
            } else {
                hideDefaultToolbar(toolbar.getMenu());
            }
            return;
        }
    }

    public void enableToolbar(){
        // TODO: return livedata from the constructor
        Log.d(getClass().getName(), "Enabling toolbar!");


        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if( R.id.copy == id) {
                    copyItems();
                    return true;
                }
                else if(R.id.delete == id) {
                    deleteItems();
                    return true;
                }
                else if(R.id.make_call == id) {
                    makeCall();
                    return true;
                }
                return false;
            }
        });
    }

    public void e2eTestHandshake() {
//
//        try {
//            String testMSISDN = "+237123456789";
//            SecurityDH securityDH = new SecurityDH(getApplicationContext());
//
//            // Transmission takes place here
//            byte[] pubKeyEncodedAlice = dhAgreementInitiation();
//            byte[] pubKeyEncodedBob = dhAgreementInitiationFromWithAlice(pubKeyEncodedAlice);
//
//            byte[][] txAgreementKeyAlice = SecurityHelpers.txAgreementFormatter(pubKeyEncodedAlice);
//            byte[][] txAgreementKeyBob = SecurityHelpers.txAgreementFormatter(pubKeyEncodedBob);
//
//            // Receiving transmission
//            byte[] rxAgreementKeyAliceFromBob = SecurityHelpers.rxAgreementFormatter(txAgreementKeyBob);
//            byte[] rxAgreementKeyBobFromAlice = SecurityHelpers.rxAgreementFormatter(txAgreementKeyAlice);
//
//            Log.d(getLocalClassName(), "Alice Pub key size: " + pubKeyEncodedAlice.length);
//            Log.d(getLocalClassName(), "Bob Pub key size: " + pubKeyEncodedBob.length);
//
//            Log.d(getLocalClassName(), "Alice Tx Pub key size - 0: " + txAgreementKeyAlice[0].length);
//            Log.d(getLocalClassName(), "Alice Tx Pub key size - 1: " + txAgreementKeyAlice[1].length);
//
//            Log.d(getLocalClassName(), "Bob Tx Pub key size - 0: " + txAgreementKeyBob[0].length);
//            Log.d(getLocalClassName(), "Bob Tx Pub key size - 1: " + txAgreementKeyBob[1].length);
//
//            Log.d(getLocalClassName(), "Alice Rx Pub key size: " + rxAgreementKeyAliceFromBob.length);
//            Log.d(getLocalClassName(), "Bob Rx Pub key size: " + rxAgreementKeyBobFromAlice.length);
//
//            byte[] secretsAlice = securityDH.getSecretKey(rxAgreementKeyAliceFromBob, testMSISDN);
//            byte[] secretsBob = securityDH.getSecretKey(rxAgreementKeyBobFromAlice, testMSISDN);
//
//            Log.d(getLocalClassName(), "Alice: " + Base64.encodeToString(secretsAlice, Base64.DEFAULT));
//            Log.d(getLocalClassName(), "Bob: " + Base64.encodeToString(secretsBob, Base64.DEFAULT));
//
//            String test = "hello world";
//
//            List<byte[]> encryptedAlice = SecurityDH.encryptAES(test.getBytes(StandardCharsets.UTF_8), secretsAlice);
//            List<byte[]> encryptedBob = SecurityDH.encryptAES(test.getBytes(StandardCharsets.UTF_8), secretsBob);
//
//            byte[] decryptedAlice = SecurityDH.decryptAES(secretsAlice, encryptedAlice.get(0), encryptedAlice.get(1));
//            byte[] decryptedBob = SecurityDH.decryptAES(secretsBob, encryptedBob.get(0), encryptedBob.get(1));
//
//            Log.d(getLocalClassName(), "Alice decrypted: " + new String(decryptedAlice, StandardCharsets.UTF_8));
//            Log.d(getLocalClassName(), "Bob decrypted: " + new String(decryptedBob, StandardCharsets.UTF_8));
//        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
//                 InvalidKeyException | NoSuchProviderException | OperatorCreationException e) {
//            throw new RuntimeException(e);
//        } catch (InvalidKeySpecException e) {
//            throw new RuntimeException(e);
//        } catch (CertificateException e) {
//            throw new RuntimeException(e);
//        } catch (KeyStoreException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (GeneralSecurityException e) {
//            throw new RuntimeException(e);
//        }
    }
    public byte[] dhAgreementInitiation() throws GeneralSecurityException, IOException {
        SecurityDH securityDH = new SecurityDH(getApplicationContext());
        PublicKey publicKey = securityDH.generateKeyPair(getApplicationContext(), address);
        return publicKey.getEncoded();
    }
}