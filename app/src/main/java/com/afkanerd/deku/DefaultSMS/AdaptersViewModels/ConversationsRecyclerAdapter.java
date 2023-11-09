package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.graphics.Color;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagingDataAdapter;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationReceivedViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationSentViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ConversationTemplateViewHandler;
import com.afkanerd.deku.DefaultSMS.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationsRecyclerAdapter extends PagingDataAdapter<Conversation, ConversationTemplateViewHandler> {
    public MutableLiveData<HashMap<Long, ConversationTemplateViewHandler>> mutableSelectedItems;
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
        this.mutableSelectedItems = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public ConversationTemplateViewHandler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
        LayoutInflater inflater = LayoutInflater.from(this.context);

        if( viewType == TIMESTAMP_MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampConversationReceivedViewHandler(view);
        }
        else if( viewType == MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampConversationSentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.KeySentViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampKeySentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.KeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampKeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampKeyReceivedStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedMiddleViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampKeySentStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentMiddleViewHandler(view);
        }

        View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
        return new ConversationSentViewHandler(view);
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
    public void onBindViewHolder(@NonNull ConversationTemplateViewHandler holder, int position) {
        final Conversation conversation = getItem(position);
        if(conversation == null) {
            return;
        }

        if(holder instanceof ConversationReceivedViewHandler) {
            ConversationReceivedViewHandler conversationReceivedViewHandler = (ConversationReceivedViewHandler) holder;
            conversationReceivedViewHandler.bind(conversation);
            if(position == 0) {
                conversationReceivedViewHandler.date.setVisibility(View.VISIBLE);
            }
        }

        else if(holder instanceof ConversationSentViewHandler){
            ConversationSentViewHandler conversationSentViewHandler = (ConversationSentViewHandler) holder;
            conversationSentViewHandler.bind(conversation);
            if(position == 0) {
                conversationSentViewHandler.date.setVisibility(View.VISIBLE);
                conversationSentViewHandler.sentMessageStatus.setVisibility(View.VISIBLE);
            }
        }

        setOnLongClickListeners(holder);
        setOnClickListeners(holder);
    }

    private void addSelectedItems(ConversationTemplateViewHandler holder) {
        HashMap<Long, ConversationTemplateViewHandler> selectedItems =  mutableSelectedItems.getValue();
        if(selectedItems == null)
            selectedItems = new HashMap<>();
        selectedItems.put(holder.getId(), holder);
        holder.itemView.setActivated(true);
        holder.activate();
        mutableSelectedItems.setValue(selectedItems);
    }

    private void removeSelectedItems(ConversationTemplateViewHandler holder) {
        HashMap<Long, ConversationTemplateViewHandler> selectedItems =  mutableSelectedItems.getValue();
        if(selectedItems != null) {
            selectedItems.remove(holder.getId());
            holder.itemView.setActivated(false);
            holder.deactivate();
            mutableSelectedItems.setValue(selectedItems);
        }
    }

    private void setOnClickListeners(ConversationTemplateViewHandler holder) {
        holder.getContainerLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mutableSelectedItems != null && mutableSelectedItems.getValue() != null) {
                    if(mutableSelectedItems.getValue().containsKey(holder.getId())) {
                        Log.d(getClass().getName(), "Removing item");
                        removeSelectedItems(holder);
                    } else if(!mutableSelectedItems.getValue().isEmpty()){
                        addSelectedItems(holder);
                    }
                }
            }
        });
    }

    private void setOnLongClickListeners(ConversationTemplateViewHandler holder) {
        holder.getContainerLayout().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(holder.itemView.isActivated()) {
                    removeSelectedItems(holder);
                } else {
                    addSelectedItems(holder);
                }
                return true;
            }
        });
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

    public void resetAllSelectedItems() {
        for(Map.Entry<Long, ConversationTemplateViewHandler> entry :
                mutableSelectedItems.getValue().entrySet()) {
            entry.getValue().itemView.setActivated(false);
            entry.getValue().deactivate();

            notifyItemChanged(entry.getValue().getAbsoluteAdapterPosition());
        }
        mutableSelectedItems.setValue(null);
    }
}
