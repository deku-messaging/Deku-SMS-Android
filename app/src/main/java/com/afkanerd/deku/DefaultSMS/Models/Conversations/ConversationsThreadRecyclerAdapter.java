package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.afkanerd.deku.DefaultSMS.ArchivedMessagesActivity;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ReceivedMessagesViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.SentMessagesViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.TemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSMetaEntity;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.afkanerd.deku.Router.Router.RouterActivity;
import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConversationsThreadRecyclerAdapter extends RecyclerView.Adapter<TemplateViewHolder> {

    private final AsyncListDiffer<Conversations> mDiffer = new AsyncListDiffer<>(this, Conversations.DIFF_CALLBACK);

    Context context;
    Boolean isSearch = false;
    String searchString = "";
    RouterActivity routerActivity;
    ArchivedMessagesActivity archivedMessagesActivity;

    public MutableLiveData<Set<TemplateViewHolder>> selectedItems = new MutableLiveData<>();
    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    private final int RECEIVED_VIEW_TYPE = 1;
    private final int RECEIVED_UNREAD_VIEW_TYPE = 2;
    private final int RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE = 3;
    private final int RECEIVED_ENCRYPTED_VIEW_TYPE = 4;

    private final int SENT_VIEW_TYPE = 5;
    private final int SENT_UNREAD_VIEW_TYPE = 6;
    private final int SENT_ENCRYPTED_UNREAD_VIEW_TYPE = 7;
    private final int SENT_ENCRYPTED_VIEW_TYPE = 8;


    RecyclerView.ViewCacheExtension viewCacheExtension;
    public ConversationsThreadRecyclerAdapter(Context context) {
       this.context = context;
    }

    public ConversationsThreadRecyclerAdapter(Context context, RecyclerView.ViewCacheExtension viewCacheExtension) {
        this.context = context;
        this.viewCacheExtension = viewCacheExtension;
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
    public long getItemId(int position) {
        return Long.parseLong(mDiffer.getCurrentList().get(position).THREAD_ID);
        // return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        Conversations conversations = mDiffer.getCurrentList().get(position);
        SMSMetaEntity smsMetaEntity = conversations.getNewestMessage();

        String snippet = conversations.SNIPPET;
        int type = smsMetaEntity.getNewestType();

        if(SecurityHelpers.containersWaterMark(snippet) || SecurityHelpers.isKeyExchange(snippet)) {
            if(!smsMetaEntity.getNewestIsRead()) {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_UNREAD_VIEW_TYPE;
                else
                    return RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE;
            }
            else {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_VIEW_TYPE;
                else
                    return RECEIVED_ENCRYPTED_VIEW_TYPE;
            }
        }
        else {
            if(!smsMetaEntity.getNewestIsRead()) {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_UNREAD_VIEW_TYPE;
                else
                    return RECEIVED_UNREAD_VIEW_TYPE;
            }else {
                if(type != MESSAGE_TYPE_INBOX) {
                    return SENT_VIEW_TYPE;
                }
            }
        }

        return RECEIVED_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Conversations conversation = mDiffer.getCurrentList().get(position);
        holder.id = conversation.THREAD_ID;

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
                singleMessageThreadIntent.putExtra(SMSMetaEntity.THREAD_ID, conversation.THREAD_ID);
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

        holder.init(conversation, onClickListener, onLongClickListener);
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

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<Conversations> list) {
        if(routerActivity != null) {
            workManagerFactories();
        }
        mDiffer.submitList(list);
    }

    public void submitList(List<Conversations> list, String searchString) {
        this.searchString = searchString;
        mDiffer.submitList(list);
    }

    private String getSMSFromWorkInfo(WorkInfo workInfo) {
        String[] tags = Helpers.convertSetToStringArray(workInfo.getTags());
        String messageId = "";
        for(int i = 0; i< tags.length; ++i) {
            if (tags[i].contains("swob.work.id")) {
                tags = tags[i].split("\\.");
                messageId = tags[tags.length - 1];
                return messageId;
            }
        }
        return messageId;
    }

    private void workManagerFactories() {
//
//        WorkQuery workQuery = WorkQuery.Builder
//                .fromTags(Collections.singletonList(IncomingTextSMSBroadcastReceiver.TAG_NAME))
//                .addStates(Arrays.asList(
//                        WorkInfo.State.ENQUEUED,
//                        WorkInfo.State.FAILED,
//                        WorkInfo.State.CANCELLED,
//                        WorkInfo.State.SUCCEEDED,
//                        WorkInfo.State.RUNNING))
//                .build();
//
//        workers = workManager.getWorkInfosLiveData(workQuery);
//        workers.observe(routerActivity, new Observer<List<WorkInfo>>() {
//            @Override
//            public void onChanged(List<WorkInfo> workInfos) {
//                if(workInfos.isEmpty())
//                    return;
//
//                List<SMS> smsList = new ArrayList<>(mDiffer.getCurrentList());
//
//                for(WorkInfo workInfo: workInfos) {
//                    String messageId = getSMSFromWorkInfo(workInfo);
//                    for(int i=0;i<mDiffer.getCurrentList().size();++i)
//                        if(mDiffer.getCurrentList().get(i).id.equals(messageId)) {
//                            SMS sms = smsList.get(i);
//                            conversation.routerStatus = workInfo.getState().name();
//                            smsList.set(i, sms);
//                            break;
//                        }
//                }
//                mDiffer.submitList(smsList);
//                notifyDataSetChanged();
//            }
//        });
    }
}
