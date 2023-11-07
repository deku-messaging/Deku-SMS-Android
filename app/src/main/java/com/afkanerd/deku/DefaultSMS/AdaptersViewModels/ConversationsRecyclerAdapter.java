package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.MessageReceivedViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.MessageSentViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.getstream.avatarview.AvatarView;

public class ConversationsRecyclerAdapter extends PagingDataAdapter<Conversation, RecyclerView.ViewHolder> {
//public class ConversationsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Toolbar toolbar;
    String highlightedText;
    View highlightedView;

    private int selectedItemAbsPosition = RecyclerView.NO_POSITION;
    public LiveData<HashMap<String, RecyclerView.ViewHolder>> selectedItem = new MutableLiveData<>();
    MutableLiveData<HashMap<String, RecyclerView.ViewHolder>> mutableSelectedItems = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedMessage = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedDataMessage = new MutableLiveData<>();

    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;

    final int MESSAGE_KEY_INBOX = 400;
    final int MESSAGE_START_TYPE_INBOX = 401;
    final int MESSAGE_MIDDLE_TYPE_INBOX = 402;
    final int MESSAGE_END_TYPE_INBOX = 403;

    final int TIMESTAMP_MESSAGE_START_TYPE_INBOX = 601;
    final int TIMESTAMP_MESSAGE_TYPE_INBOX = 600;
    final int TIMESTAMP_KEY_TYPE_INBOX = 800;

    final int MESSAGE_KEY_OUTBOX = 500;
    final int TIMESTAMP_MESSAGE_TYPE_OUTBOX = 700;
    final int TIMESTAMP_KEY_TYPE_OUTBOX = 900;

    final int TIMESTAMP_MESSAGE_START_TYPE_OUTBOX = 504;
    final int MESSAGE_START_TYPE_OUTBOX = 501;
    final int MESSAGE_MIDDLE_TYPE_OUTBOX = 502;
    final int MESSAGE_END_TYPE_OUTBOX = 503;

    public String searchString;

    Context context;

    public ConversationsRecyclerAdapter(Context context) throws GeneralSecurityException, IOException {
        super(Conversation.DIFF_CALLBACK);
        this.context = context;
        this.selectedItem = mutableSelectedItems;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
        LayoutInflater inflater = LayoutInflater.from(this.context);

        if( viewType == TIMESTAMP_MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.TimestampMessageReceivedViewHandler(view);
        }
        else if( viewType == MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.TimestampMessageSentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.KeySentViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.TimestampKeySentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.KeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.TimestampKeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.TimestampKeyReceivedStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.MessageReceivedStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.MessageReceivedEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler.MessageReceivedMiddleViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.TimestampKeySentStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.MessageSentStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.MessageSentEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentViewHandler.MessageSentMiddleViewHandler(view);
        }

        View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
        return new MessageSentViewHandler(view);
    }

    public Spannable highlightSubstringYellow(String text) {
        // Find all occurrences of the substring in the text.
        List<Integer> startIndices = new ArrayList<>();
        int index = text.indexOf(searchString);
        while (index >= 0) {
            startIndices.add(index);
            index = text.indexOf(searchString, index + searchString.length());
        }

        // Create a SpannableString object.
        SpannableString spannableString = new SpannableString(text);

        // Set the foreground color of the substring to yellow.
        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.YELLOW);
        for (int startIndex : startIndices) {
            spannableString.setSpan(backgroundColorSpan, startIndex, startIndex + searchString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Conversation conversation = getItem(position);
        if(conversation == null) {
            return;
        }

        if(holder instanceof MessageReceivedViewHandler) {
            MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
            messageReceivedViewHandler.bind(conversation);
            if(position == 0) {
                messageReceivedViewHandler.date.setVisibility(View.VISIBLE);
            }
        }

        else if(holder instanceof MessageSentViewHandler){
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;
            messageSentViewHandler.bind(conversation);
            if(position == 0) {
                messageSentViewHandler.date.setVisibility(View.VISIBLE);
                messageSentViewHandler.sentMessageStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Conversation sms = peek(position);
        if(sms == null)
            return super.getItemViewType(position);

//        boolean isEncryptionKey = SecurityHelpers.isKeyExchange(sms.getBody());

        int oldestItemPos = snapshot().getSize() - 1;
        int newestItemPos = 0;

//        if(isEncryptionKey) {
//            if(position == oldestItemPos || snapshot().size() < 2 ||
//                    (position > (oldestItemPos -1) &&
//                            Helpers.isSameMinute(Long.parseLong(sms.getDate()),
//                                    Long.parseLong(((Conversation) peek(position -1)).getDate())))) {
//                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
//                        TIMESTAMP_KEY_TYPE_INBOX : TIMESTAMP_KEY_TYPE_OUTBOX;
//            }
//            else {
//                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
//                        MESSAGE_KEY_INBOX : MESSAGE_KEY_OUTBOX;
//            }
//        }

        if(snapshot().getSize() < 2) {
            return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                    TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
        }


        if(position == oldestItemPos) { // - minus
            Conversation secondMessage = (Conversation) peek(position - 1);
            if(sms.getType() == secondMessage.getType() && Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                    Long.parseLong(secondMessage.getDate()))) {
                Log.d(getClass().getName(), "Yes oldest timestamp");
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
            }
            else {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
        }

        if(position > 0) {
            Conversation secondMessage = (Conversation) peek(position + 1);
            if(secondMessage == null)
                return super.getItemViewType(position);
            Conversation thirdMessage = (Conversation) peek(position - 1);
            if(!Helpers.isSameHour(Long.parseLong(sms.getDate()), Long.parseLong(secondMessage.getDate()))) {
                if(sms.getType() == thirdMessage.getType() && Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                        Long.parseLong(thirdMessage.getDate())))
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
                else
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
            if(sms.getType() == thirdMessage.getType() && Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                    Long.parseLong(thirdMessage.getDate()))) {
                if(sms.getType() != secondMessage.getType() || !Helpers.isSameMinute(
                        Long.parseLong(sms.getDate()), Long.parseLong(secondMessage.getDate()))) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_START_TYPE_INBOX : MESSAGE_START_TYPE_OUTBOX;
                }
            }
            if(sms.getType() == secondMessage.getType() && sms.getType() == thirdMessage.getType()) {
                if(Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                        Long.parseLong(secondMessage.getDate())) &&
                        Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                                Long.parseLong(thirdMessage.getDate()))) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_MIDDLE_TYPE_INBOX : MESSAGE_MIDDLE_TYPE_OUTBOX;
                }
            }
            if(sms.getType() == secondMessage.getType() && Helpers.isSameMinute(Long.parseLong(sms.getDate()),
                    Long.parseLong(secondMessage.getDate()))) {
                if(sms.getType() != thirdMessage.getType() || !Helpers.isSameMinute(
                        Long.parseLong(sms.getDate()), Long.parseLong(thirdMessage.getDate()))) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
                }
            }
        }
        if(position == newestItemPos) { // - minus
            Conversation secondMessage = (Conversation) peek(position + 1);

            if(secondMessage == null)
                return super.getItemViewType(position);
            if(!Helpers.isSameHour(Long.parseLong(sms.getDate()), Long.parseLong(secondMessage.getDate()))) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }

            if(sms.getType() == secondMessage.getType() && Helpers.isSameMinute(
                    Long.parseLong(sms.getDate()), Long.parseLong(secondMessage.getDate()))) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
            }
        }

        return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                MESSAGE_TYPE_INBOX : MESSAGE_TYPE_OUTBOX;
    }


}
