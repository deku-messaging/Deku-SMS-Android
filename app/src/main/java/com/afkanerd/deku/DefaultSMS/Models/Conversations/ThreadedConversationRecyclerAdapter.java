package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagingDataAdapter;

import com.afkanerd.deku.DefaultSMS.ArchivedMessagesActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ReceivedMessagesViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.SentMessagesViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.TemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.R;


import java.util.HashSet;
import java.util.Set;

public class ThreadedConversationRecyclerAdapter extends PagingDataAdapter<ThreadedConversations, TemplateViewHolder> {

    Context context;
    Boolean isSearch = false;
    public String searchString = "";
    ArchivedMessagesActivity archivedMessagesActivity;

    public MutableLiveData<Set<TemplateViewHolder>> selectedItems = new MutableLiveData<>();
    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    public final static int RECEIVED_VIEW_TYPE = 1;
    public final static int RECEIVED_UNREAD_VIEW_TYPE = 2;
    public final static int RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE = 3;
    public final static int RECEIVED_ENCRYPTED_VIEW_TYPE = 4;

    public final static int SENT_VIEW_TYPE = 5;
    public final static int SENT_UNREAD_VIEW_TYPE = 6;
    public final static int SENT_ENCRYPTED_UNREAD_VIEW_TYPE = 7;
    public final static int SENT_ENCRYPTED_VIEW_TYPE = 8;

    public ThreadedConversationRecyclerAdapter(Context context) {
        super(ThreadedConversations.DIFF_CALLBACK);
        this.context = context;
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);

        View view = inflater.inflate(R.layout.messages_threads_layout, parent, false);
//        View view = viewCacheExtension.getViewForPositionAndType(parent, 0, viewType);
        if(viewType == (RECEIVED_UNREAD_VIEW_TYPE))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderUnread(view);
        else if(viewType == (SENT_UNREAD_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderUnread(view);

        else if(viewType == (RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderEncryptedUnread(view);
         else if(viewType == (SENT_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new SentMessagesViewHolder.SentViewHolderEncryptedUnread(view);

        else if(viewType == (RECEIVED_VIEW_TYPE))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderRead(view);
        else if(viewType == (SENT_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderRead(view);

        else if(viewType == (SENT_ENCRYPTED_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderEncryptedRead(view);

        return new ReceivedMessagesViewHolder.ReceivedViewHolderEncryptedRead(view);
    }


    @Override
    public int getItemViewType(int position) {
//        Conversations conversations = mDiffer.getCurrentList().get(position);
//        ConversationHandler smsMetaEntity = conversations.getNewestMessage();
//        ThreadedConversations threadedConversations = peek(position);
        return TemplateViewHolder.getViewType(position, snapshot().getItems());
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        ThreadedConversations threadedConversations = getItem(position);
        String threadId = String.valueOf(threadedConversations.getThread_id());

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<TemplateViewHolder> _selectedItems = selectedItems.getValue();
                if(_selectedItems != null) {
                    if(_selectedItems.contains(holder)) {
                        _selectedItems.remove(holder);
                        selectedItems.postValue(_selectedItems);
                        holder.unHighlight();
                        return;
                    }
                    else if(!_selectedItems.isEmpty()){
                        _selectedItems.add(holder);
                        selectedItems.postValue(_selectedItems);
                        holder.highlight();
                        return;
                    }
                }

                Intent singleMessageThreadIntent = new Intent(context, ConversationActivity.class);
                singleMessageThreadIntent.putExtra(Conversation.THREAD_ID, threadId);
                context.startActivity(singleMessageThreadIntent);
            }
        };
//
        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Set<TemplateViewHolder> _selectedItems = selectedItems.getValue() == null ?
                        new HashSet<>() : selectedItems.getValue();
                _selectedItems.add(holder);
                selectedItems.postValue(_selectedItems);
                holder.highlight();
                return true;
            }
        };

        holder.bind(threadedConversations, onClickListener, onLongClickListener);
   }

    public void resetAllSelectedItems() {
        Set<TemplateViewHolder> items = selectedItems.getValue();
        if(items != null) {
            for(TemplateViewHolder viewHolder : items) {
                viewHolder.unHighlight();
            }
        }
        selectedItems.setValue(new HashSet<>());
    }
}
