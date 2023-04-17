package com.example.swob_deku.Models.Messages;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.example.swob_deku.ArchivedMessagesActivity;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.example.swob_deku.RouterActivity;
import com.example.swob_deku.BroadcastSMSTextActivity;
import com.example.swob_deku.SMSSendActivity;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.getstream.avatarview.AvatarView;

public class MessagesThreadRecyclerAdapter extends RecyclerView.Adapter<MessagesThreadRecyclerAdapter.ViewHolder> {

    private final AsyncListDiffer<SMS> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    Context context;
    int renderLayout;
    Boolean isSearch = false;
    String searchString = "";
    RouterActivity routerActivity;
    ArchivedMessagesActivity archivedMessagesActivity;

    WorkManager workManager;
    LiveData<List<WorkInfo>> workers;

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    private int UNREAD_VIEW_TYPE = 1;

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

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Collections.singletonList(BroadcastSMSTextActivity.TAG_NAME))
                .addStates(Arrays.asList(
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED,
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.RUNNING))
                .build();

        workers = workManager.getWorkInfosLiveData(workQuery);
        workers.observe(routerActivity, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.isEmpty())
                    return;

                List<SMS> smsList = new ArrayList<>(mDiffer.getCurrentList());

                for(WorkInfo workInfo: workInfos) {
                    String messageId = getSMSFromWorkInfo(workInfo);
                    for(int i=0;i<mDiffer.getCurrentList().size();++i)
                        if(mDiffer.getCurrentList().get(i).id.equals(messageId)) {
                            SMS sms = smsList.get(i);
                            sms.routerStatus = workInfo.getState().name();
                            smsList.set(i, sms);
                            break;
                        }
                }
                mDiffer.submitList(smsList);
                notifyDataSetChanged();
            }
        });
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout) {
       this.context = context;
       this.renderLayout = renderLayout;
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout, Boolean isSearch,
                                         String searchString) {
        this.context = context;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout, Boolean isSearch,
                                         String searchString, RouterActivity routerActivity) {
        this.context = context;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
        this.routerActivity = routerActivity;

        workManager = WorkManager.getInstance(context);
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout, Boolean isSearch,
                                         String searchString, ArchivedMessagesActivity archivedMessagesActivity) {
        this.context = context;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
        this.archivedMessagesActivity = archivedMessagesActivity;

        workManager = WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public MessagesThreadRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(this.renderLayout, parent, false);

        MessagesThreadRecyclerAdapter.ViewHolder viewHolder = new ViewHolder(view);
        if(viewType == UNREAD_VIEW_TYPE) {
            viewHolder.address.setTypeface(Typeface.DEFAULT_BOLD);
            viewHolder.snippet.setTypeface(Typeface.DEFAULT_BOLD);
            viewHolder.date.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return viewHolder;
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public int getItemViewType(int position) {
        SMS sms = mDiffer.getCurrentList().get(position);
        if(SMSHandler.hasUnreadMessages(context, sms.getThreadId())) {
//            holder.address.setTypeface(Typeface.DEFAULT_BOLD);
//            holder.snippet.setTypeface(Typeface.DEFAULT_BOLD);
            return UNREAD_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SMS sms = mDiffer.getCurrentList().get(position);
        final String smsThreadId = sms.getThreadId();
        holder.id = smsThreadId;

        String message = sms.getBody();

        Spannable spannable = Spannable.Factory.getInstance().newSpannable(message);

        if(sms.getType() == MESSAGE_TYPE_SENT) {
            String entityTitle = context.getString(R.string.messages_thread_you);
            spannable = Spannable.Factory.getInstance().newSpannable( entityTitle + message);
            message = spannable.toString();

            StyleSpan ItalicSpan = new StyleSpan(Typeface.ITALIC);

            spannable.setSpan(ItalicSpan, 0, entityTitle.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(
                    context.getColor(R.color.primary_highlight_color));

            spannable.setSpan(foregroundColorSpan, 0, entityTitle.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if(!this.searchString.isEmpty()) {
            String lowercaseMessage = message.toLowerCase();
            String lowercaseSearchString = searchString.toLowerCase();

            for(int index = lowercaseMessage.indexOf(lowercaseSearchString);
                index >=0; index = lowercaseMessage.indexOf(lowercaseSearchString, index + 1)) {

                spannable.setSpan(new BackgroundColorSpan(context.getResources().getColor(
                        R.color.highlight_yellow, context.getTheme())),
                        index, index + (this.searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(
                        R.color.black, context.getTheme())),
                        index, index + (this.searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        holder.snippet.setText(spannable);

        holder.state.setText(sms.getRouterStatus());

        String address = sms.getAddress();

        if(checkPermissionToReadContacts() && !address.isEmpty()) {
            String addressInPhone = Contacts.retrieveContactName(context, address);

            if(!addressInPhone.isEmpty() && !addressInPhone.equals("null")) {
                address = addressInPhone;
                holder.contactPhoto.setAvatarInitials(address.substring(0, 1));
            }
        }

        holder.address.setText(address);

        String date = Helpers.formatDate(context, Long.parseLong(sms.getDate()));
        holder.date.setText(date);

        if(routerActivity != null && !sms.routingUrls.isEmpty()) {
            holder.routingURLText.setVisibility(View.VISIBLE);
            holder.routingUrl.setVisibility(View.VISIBLE);

            StringBuilder routingUrl = new StringBuilder();
            for(int i=0;i<sms.routingUrls.size(); ++i) {
                if (routingUrl.length() > 0)
                    routingUrl.append(", ");
                routingUrl.append(sms.routingUrls.get(i));
            }
            holder.routingUrl.setText(routingUrl);
        }
        else {
            holder.routingURLText.setVisibility(View.GONE);
        }

        final int absolutePosition = holder.getAbsoluteAdapterPosition();
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent singleMessageThreadIntent = new Intent(context, SMSSendActivity.class);
                singleMessageThreadIntent.putExtra(SMSSendActivity.ADDRESS, sms.getAddress());
                singleMessageThreadIntent.putExtra(SMSSendActivity.THREAD_ID, sms.getThreadId());

                if (searchString != null && !searchString.isEmpty()) {
                    int calculatedOffset = SMSHandler.calculateOffset(context, sms.getThreadId(), sms.getId());
                    singleMessageThreadIntent
                            .putExtra(SMSSendActivity.ID, sms.getId())
                            .putExtra(SMSSendActivity.SEARCH_STRING, searchString)
                            .putExtra(SMSSendActivity.SEARCH_OFFSET, calculatedOffset)
                            .putExtra(SMSSendActivity.SEARCH_POSITION, absolutePosition);
                }

                context.startActivity(singleMessageThreadIntent);
            }
        };

        holder.layout.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<SMS> list) {
        if(routerActivity != null) {
            workManagerFactories();
        }

        mDiffer.submitList(list);
    }

    public void submitList(List<SMS> list, String searchString) {
        this.searchString = searchString;
        mDiffer.submitList(list);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public String id;
        public TextView snippet;
        public TextView address;
        public TextView date;
        public TextView state;
        public TextView routingUrl;
        public TextView routingURLText;
        public AvatarView contactPhoto;

        ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            snippet = itemView.findViewById(R.id.messages_thread_text);
            address = itemView.findViewById(R.id.messages_thread_address_text);
            date = itemView.findViewById(R.id.messages_thread_date);
            layout = itemView.findViewById(R.id.messages_threads_layout);
            state = itemView.findViewById(R.id.messages_route_state);
            routingUrl = itemView.findViewById(R.id.message_route_url);
            routingURLText = itemView.findViewById(R.id.message_route_status);
            contactPhoto = itemView.findViewById(R.id.messages_threads_contact_photo);
        }
    }

    // TODO:
    public static final DiffUtil.ItemCallback<SMS> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SMS>() {
                @Override
                public boolean areItemsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
