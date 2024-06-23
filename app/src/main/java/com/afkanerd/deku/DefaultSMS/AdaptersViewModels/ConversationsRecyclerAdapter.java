package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import static java.sql.DriverManager.println;

import android.provider.Telephony;
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

    ConversationSentViewHandler lastSentItem;
    ConversationReceivedViewHandler lastReceivedItem;

    public ConversationsRecyclerAdapter() {
        super(Conversation.Companion.getDIFF_CALLBACK());
        this.mutableSelectedItems = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public ConversationTemplateViewHandler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ConversationTemplateViewHandler returnView;

        switch(viewType) {
            case TIMESTAMP_MESSAGE_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.TimestampConversationReceivedViewHandler(
                                inflater.inflate(R.layout.layout_conversations_received,
                                        parent, false));
                break;
            case MESSAGE_TYPE_INBOX :
                returnView = new ConversationReceivedViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case TIMESTAMP_MESSAGE_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.TimestampConversationSentViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            case MESSAGE_KEY_OUTBOX:
                View view = inflater.inflate(R.layout.layout_conversations_sent, parent, false);
                returnView = new ConversationSentViewHandler.KeySentViewHandler(view);
                break;
            case TIMESTAMP_KEY_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.TimestampKeySentViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            case MESSAGE_KEY_INBOX:
                returnView = new ConversationReceivedViewHandler.KeyReceivedViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case TIMESTAMP_KEY_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.TimestampKeyReceivedViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case TIMESTAMP_MESSAGE_START_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.TimestampKeyReceivedStartViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case MESSAGE_START_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.ConversationReceivedStartViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case MESSAGE_END_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.ConversationReceivedEndViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case MESSAGE_MIDDLE_TYPE_INBOX:
                returnView = new ConversationReceivedViewHandler.ConversationReceivedMiddleViewHandler(
                        inflater.inflate(R.layout.layout_conversations_received, parent, false));
                break;
            case TIMESTAMP_MESSAGE_START_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.TimestampKeySentStartGroupViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            case MESSAGE_START_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.ConversationSentStartViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            case MESSAGE_END_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.ConversationSentEndViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            case MESSAGE_MIDDLE_TYPE_OUTBOX:
                returnView = new ConversationSentViewHandler.ConversationSentMiddleViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
                break;
            default:
                returnView = new ConversationSentViewHandler(
                        inflater.inflate(R.layout.layout_conversations_sent, parent, false));
        }

        return returnView;
    }


    @Override
    public void onBindViewHolder(@NonNull ConversationTemplateViewHandler holder, int position) {
//        Log.d(getClass().getName(), "Binding: " + holder.getAbsoluteAdapterPosition());
        final Conversation conversation = getItem(position);
        if(conversation == null) {
            return;
        }
        setOnLongClickListeners(holder);
        setOnClickListeners(holder, conversation);

        if(holder instanceof ConversationReceivedViewHandler) {
            ConversationReceivedViewHandler conversationReceivedViewHandler =
                    (ConversationReceivedViewHandler) holder;
            conversationReceivedViewHandler.bind(conversation, searchString);
            if(holder.getAbsoluteAdapterPosition() == 0) {
                if(lastReceivedItem != null)
                    lastReceivedItem.hideDetails();
                lastReceivedItem = conversationReceivedViewHandler;
                lastReceivedItem.date.setVisibility(View.VISIBLE);
            }
        }

        else if(holder instanceof ConversationSentViewHandler){
            ConversationSentViewHandler conversationSentViewHandler = (ConversationSentViewHandler) holder;
            if(holder.getAbsoluteAdapterPosition() != 0 ) {
                conversationSentViewHandler.hideDetails();
            } else conversationSentViewHandler.showDetails();
            conversationSentViewHandler.bind(conversation, searchString);
        }

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

        if(holder instanceof ConversationSentViewHandler) {
            ((ConversationSentViewHandler) holder).messageFailedIcon
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(conversation.getStatus() == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                                if(conversation.getData() != null) retryFailedDataMessage.setValue(conversation);
                                else retryFailedMessage.setValue(conversation);
                            }
                        }
            });
        }

        holder.getContainerLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mutableSelectedItems != null && mutableSelectedItems.getValue() != null) {
                    if(mutableSelectedItems.getValue().containsKey(holder.getId())) {
                        removeSelectedItems(holder);
                    } else if(!mutableSelectedItems.getValue().isEmpty()){
                        addSelectedItems(holder);
                    }
                } else if(conversation.getStatus() == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                    if(conversation.getData() != null) retryFailedDataMessage.setValue(conversation);
                    else retryFailedMessage.setValue(conversation);
                } else {
                    holder.toggleDetails();
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

    public void resetSearchItems(List<Integer> positions) {
        if(positions != null)
            for(Integer position : positions) {
                notifyItemChanged(position);
            }
    }
}
