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
    public MutableLiveData<Conversation> retryFailedMessage = new MutableLiveData<>();
    public MutableLiveData<Conversation> retryFailedDataMessage = new MutableLiveData<>();

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
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampConversationReceivedViewHandler(view);
        }
        else if( viewType == MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampConversationSentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.KeySentViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampKeySentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.KeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampKeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.TimestampKeyReceivedStartViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.conversations_received_layout, parent, false);
            return new ConversationReceivedViewHandler.ConversationReceivedMiddleViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.TimestampKeySentStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
            return new ConversationSentViewHandler.ConversationSentMiddleViewHandler(view);
        }

        View view = inflater.inflate(R.layout.conversations_sent_layout, parent, false);
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
                conversationSentViewHandler.messageStatusLinearLayoutCompact.setVisibility(View.VISIBLE);
            }
        }

        setOnLongClickListeners(holder);
        setOnClickListeners(holder, conversation);
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

    private void setOnClickListeners(ConversationTemplateViewHandler holder, Conversation conversation) {
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
                } else {
                    if(conversation.getStatus() == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                        if(conversation.getData() != null)
                            retryFailedDataMessage.setValue(conversation);
                        else
                            retryFailedMessage.setValue(conversation);
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
        Conversation conversation = peek(position);
        if(conversation == null)
            return super.getItemViewType(position);

        int oldestItemPos = snapshot().getSize() - 1;
        int newestItemPos = 0;

        if(conversation.isIs_key()) {
            if(position == oldestItemPos || snapshot().size() < 2 ||
                    (position > (oldestItemPos -1) &&
                            Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                                    Long.parseLong(peek(position -1).getDate())))) {
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_KEY_TYPE_INBOX : TIMESTAMP_KEY_TYPE_OUTBOX;
            }
            else {
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        MESSAGE_KEY_INBOX : MESSAGE_KEY_OUTBOX;
            }
        }

        if(snapshot().getSize() < 2) {
            return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                    TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
        }


        if(position == oldestItemPos) { // - minus
            Conversation secondMessage = (Conversation) peek(position - 1);
            if(conversation.getType() == secondMessage.getType() && Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                    Long.parseLong(secondMessage.getDate()))) {
                Log.d(getClass().getName(), "Yes oldest timestamp");
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
            }
            else {
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
        }

        if(position > 0) {
            Conversation secondMessage = (Conversation) peek(position + 1);
            Conversation thirdMessage = (Conversation) peek(position - 1);
            if(secondMessage == null || thirdMessage == null)
                return super.getItemViewType(position);
            if(!Helpers.isSameHour(Long.parseLong(conversation.getDate()), Long.parseLong(secondMessage.getDate()))) {
                if(conversation.getType() == thirdMessage.getType() && Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                        Long.parseLong(thirdMessage.getDate())))
                    return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
                else
                    return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
            if(conversation.getType() == thirdMessage.getType() && Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                    Long.parseLong(thirdMessage.getDate()))) {
                if(conversation.getType() != secondMessage.getType() || !Helpers.isSameMinute(
                        Long.parseLong(conversation.getDate()), Long.parseLong(secondMessage.getDate()))) {
                    return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_START_TYPE_INBOX : MESSAGE_START_TYPE_OUTBOX;
                }
            }
            if(conversation.getType() == secondMessage.getType() && conversation.getType() == thirdMessage.getType()) {
                if(Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                        Long.parseLong(secondMessage.getDate())) &&
                        Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                                Long.parseLong(thirdMessage.getDate()))) {
                    return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_MIDDLE_TYPE_INBOX : MESSAGE_MIDDLE_TYPE_OUTBOX;
                }
            }
            if(conversation.getType() == secondMessage.getType() && Helpers.isSameMinute(Long.parseLong(conversation.getDate()),
                    Long.parseLong(secondMessage.getDate()))) {
                if(conversation.getType() != thirdMessage.getType() || !Helpers.isSameMinute(
                        Long.parseLong(conversation.getDate()), Long.parseLong(thirdMessage.getDate()))) {
                    return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
                }
            }
        }
        if(position == newestItemPos) { // - minus
            Conversation secondMessage = (Conversation) peek(position + 1);

            if(secondMessage == null)
                return super.getItemViewType(position);
            if(!Helpers.isSameHour(Long.parseLong(conversation.getDate()), Long.parseLong(secondMessage.getDate()))) {
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }

            if(conversation.getType() == secondMessage.getType() && Helpers.isSameMinute(
                    Long.parseLong(conversation.getDate()), Long.parseLong(secondMessage.getDate()))) {
                return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
                        MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
            }
        }

        return (conversation.getType() == MESSAGE_TYPE_INBOX) ?
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

    public void getForce(int position) {
        getItem(position);
    }
}
