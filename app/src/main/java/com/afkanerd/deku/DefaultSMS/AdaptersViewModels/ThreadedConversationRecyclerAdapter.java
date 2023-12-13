package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

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
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsReceivedViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsSentViewHandler;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.R;


import java.util.HashMap;

public class ThreadedConversationRecyclerAdapter extends PagingDataAdapter<ThreadedConversations, ThreadedConversationsTemplateViewHolder> {

    Context context;
    Boolean isSearch = false;
    public String searchString = "";
    ArchivedMessagesActivity archivedMessagesActivity;

    public MutableLiveData<HashMap<Long, ThreadedConversationsTemplateViewHolder>> selectedItems = new MutableLiveData<>();
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

    ThreadedConversationsDao threadedConversationsDao;

    public ThreadedConversationRecyclerAdapter(Context context) {
        super(ThreadedConversations.DIFF_CALLBACK);
        this.context = context;
    }

    public ThreadedConversationRecyclerAdapter(Context context, ThreadedConversationsDao threadedConversationsDao) {
        super(ThreadedConversations.DIFF_CALLBACK);
        this.context = context;
        this.threadedConversationsDao = threadedConversationsDao;
    }

    @NonNull
    @Override
    public ThreadedConversationsTemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);

        View view = inflater.inflate(R.layout.conversations_threads_layout, parent, false);
//        View view = viewCacheExtension.getViewForPositionAndType(parent, 0, viewType);
        if(viewType == (RECEIVED_UNREAD_VIEW_TYPE))
            return new ThreadedConversationsReceivedViewHandler.ReceivedViewHolderUnreadThreadedConversations(view);
        else if(viewType == (SENT_UNREAD_VIEW_TYPE))
            return new ThreadedConversationsSentViewHandler.SentViewHolderUnreadThreadedConversations(view);

        else if(viewType == (RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new ThreadedConversationsReceivedViewHandler.ReceivedViewHolderEncryptedUnreadThreadedConversations(view);
         else if(viewType == (SENT_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new ThreadedConversationsSentViewHandler.SentViewHolderEncryptedUnreadThreadedConversations(view);

        else if(viewType == (RECEIVED_VIEW_TYPE))
            return new ThreadedConversationsReceivedViewHandler.ReceivedViewHolderReadThreadedConversations(view);
        else if(viewType == (SENT_VIEW_TYPE))
            return new ThreadedConversationsSentViewHandler.SentViewHolderReadThreadedConversations(view);

        else if(viewType == (SENT_ENCRYPTED_VIEW_TYPE))
            return new ThreadedConversationsSentViewHandler.SentViewHolderEncryptedReadThreadedConversations(view);

        return new ThreadedConversationsReceivedViewHandler.ReceivedViewHolderEncryptedReadThreadedConversations(view);
    }


    @Override
    public int getItemViewType(int position) {
        return ThreadedConversationsTemplateViewHolder.getViewType(position, snapshot().getItems());
    }

    public void markThreadRead(ThreadedConversations threadedConversations) {
        if(threadedConversationsDao != null) {
            threadedConversations.setIs_read(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    threadedConversationsDao.update(threadedConversations);
                }
            }).start();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadedConversationsTemplateViewHolder holder, int position) {
        ThreadedConversations threadedConversations = getItem(position);
        if(threadedConversations == null)
            return;
        String threadId = String.valueOf(threadedConversations.getThread_id());

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<Long, ThreadedConversationsTemplateViewHolder> _selectedItems = selectedItems.getValue();
                if(_selectedItems != null) {
                    if(_selectedItems.containsKey(holder)) {
                        ThreadedConversationsTemplateViewHolder templateViewHolder = _selectedItems.remove(holder);
                        selectedItems.setValue(_selectedItems);
                        if(templateViewHolder != null)
                            templateViewHolder.unHighlight();
                        return;
                    }
                    else if(!_selectedItems.isEmpty()){
                        _selectedItems.put(Long.valueOf(holder.id), holder);
                        selectedItems.setValue(_selectedItems);
                        holder.highlight();
                        return;
                    }
                }

//                markThreadRead(threadedConversations);

                Intent singleMessageThreadIntent = new Intent(context, ConversationActivity.class);
                singleMessageThreadIntent.putExtra(Conversation.THREAD_ID, threadId);
                context.startActivity(singleMessageThreadIntent);
            }
        };
//
        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                HashMap<Long, ThreadedConversationsTemplateViewHolder> _selectedItems = selectedItems.getValue() == null ?
                        new HashMap<>() : selectedItems.getValue();
                _selectedItems.put(Long.valueOf(holder.id), holder);
                selectedItems.setValue(_selectedItems);
                holder.highlight();
                return true;
            }
        };

        holder.bind(threadedConversations, onClickListener, onLongClickListener);
   }

    public void resetAllSelectedItems() {
        HashMap<Long, ThreadedConversationsTemplateViewHolder> items = selectedItems.getValue();
        if(items != null) {
            for(ThreadedConversationsTemplateViewHolder viewHolder : items.values()){
                viewHolder.unHighlight();
            }
        }
        selectedItems.setValue(null);
    }
}
