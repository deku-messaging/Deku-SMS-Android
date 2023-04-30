package com.example.swob_deku.Models.Messages;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.RouterActivity;
import com.example.swob_deku.BroadcastSMSTextActivity;
import com.example.swob_deku.SMSSendActivity;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    public MutableLiveData<List<String>> selectedItems = new MutableLiveData<>();

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    private final int CONTACT_VIEW_TYPE = 136;
    private final int NOT_CONTACT_VIEW_TYPE = 235;
    private final int RECEIVED_VIEW_TYPE = 1;
    private final int RECEIVED_UNREAD_VIEW_TYPE = 2;
    private final int RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE = 3;
    private final int RECEIVED_ENCRYPTED_VIEW_TYPE = 4;

    private final int SENT_VIEW_TYPE = 5;
    private final int SENT_UNREAD_VIEW_TYPE = 6;
    private final int SENT_ENCRYPTED_UNREAD_VIEW_TYPE = 7;
    private final int SENT_ENCRYPTED_VIEW_TYPE = 8;

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

        if(viewType == (RECEIVED_UNREAD_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new UnreadViewHolder(view, true);
        else if(viewType == (RECEIVED_UNREAD_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new UnreadViewHolder(view, false);

        else if(viewType == (SENT_UNREAD_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new SentViewHolderUnread(view, true);
        else if(viewType == (SENT_UNREAD_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new SentViewHolderUnread(view, false);

        else if(viewType == (RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new UnreadEncryptedViewHolder(view, true);
        else if(viewType == (RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new UnreadEncryptedViewHolder(view, false);

         else if(viewType == (SENT_ENCRYPTED_UNREAD_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new SentEncryptedViewHolderUnread(view, true);
        else if(viewType == (SENT_ENCRYPTED_UNREAD_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new SentEncryptedViewHolderUnread(view, false);

        else if(viewType == (SENT_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new SentViewHolder(view, true);
        else if(viewType == (SENT_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new SentViewHolder(view, false);

         else if(viewType == (RECEIVED_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new ViewHolder(view, true);
        else if(viewType == (RECEIVED_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new ViewHolder(view, false);

        else if(viewType == (SENT_ENCRYPTED_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new SentEncryptedViewHolder(view, true);
        else if(viewType == (SENT_ENCRYPTED_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new SentEncryptedViewHolder(view, false);

        else if(viewType == (RECEIVED_ENCRYPTED_VIEW_TYPE + CONTACT_VIEW_TYPE))
            return new EncryptedViewHolder(view, true);
        else if(viewType == (RECEIVED_ENCRYPTED_VIEW_TYPE + NOT_CONTACT_VIEW_TYPE))
            return new EncryptedViewHolder(view, false);


        return new ViewHolder(view, false);
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    public boolean isContact(SMS sms) {
        String addressInPhone = Contacts.retrieveContactName(context, sms.getAddress());
        return !addressInPhone.isEmpty() && !addressInPhone.equals("null");
    }
    @Override
    public int getItemViewType(int position) {
        SMS sms = mDiffer.getCurrentList().get(position);

        boolean smsIsContact = isContact(sms);

        int type = smsIsContact ? CONTACT_VIEW_TYPE : NOT_CONTACT_VIEW_TYPE;

        if(SecurityHelpers.containersWaterMark(sms.getBody())) {
            if(SMSHandler.hasUnreadMessages(context, sms.getThreadId())) {
                if(sms.getType() != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_UNREAD_VIEW_TYPE + type;
                else
                    return RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE + type;
            }
            else {
                if(sms.getType() != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_VIEW_TYPE + type;
                else
                    return RECEIVED_ENCRYPTED_VIEW_TYPE + type;
            }
        } else {
            if(SMSHandler.hasUnreadMessages(context, sms.getThreadId())) {
                if(sms.getType() != MESSAGE_TYPE_INBOX)
                    return SENT_UNREAD_VIEW_TYPE + type;
                else
                    return RECEIVED_UNREAD_VIEW_TYPE + type;
            }else {
                if(sms.getType() != MESSAGE_TYPE_INBOX) {
                    return SENT_VIEW_TYPE + type;
                }
            }
        }

        return RECEIVED_VIEW_TYPE + type;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final int absolutePosition = holder.getAbsoluteAdapterPosition();
        SMS sms = mDiffer.getCurrentList().get(absolutePosition);
        holder.id = sms.getThreadId();

        String address = sms.getAddress();

        if(holder.isContact) {
            String addressInPhone = Contacts.retrieveContactName(context, sms.getAddress());
            if (!addressInPhone.isEmpty() && !addressInPhone.equals("null")) {
                address = addressInPhone;
                final int color = Helpers.generateColor(address.charAt(address.length() -1));
                holder.contactInitials.setAvatarInitials(address.substring(0, 1));

//                final int colorValue = (int) address.charAt(0);
//                final int red = colorValue + 10;
//                final int green = (int) (red * 1.5);
////                final int blue = address.length() > 1 ? colorValue + (int) address.charAt(1) : colorValue;
//                final int blue = (int) (green * 1.25);

//                final int randomColor = Color.rgb(red, green, blue);
                holder.contactInitials.setAvatarInitialsBackgroundColor(color);
            }
        } else {
//            Drawable drawable = holder.contactPhoto.getDrawable();
//            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
//            holder.contactPhoto.setImageDrawable(drawable);
        }

        holder.address.setText(address);

        String date = Helpers.formatDate(context, Long.parseLong(sms.getDate()));
        holder.date.setText(date);

        if(!(holder instanceof EncryptedViewHolder) && !(holder instanceof SentEncryptedViewHolder)
                && !(holder instanceof UnreadEncryptedViewHolder) && !(holder instanceof SentEncryptedViewHolderUnread)) {
            String message = sms.getBody();
            if(!this.searchString.isEmpty()) {
                Spannable spannable = Spannable.Factory.getInstance().newSpannable(message);
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
                holder.snippet.setText(spannable);
            } else holder.snippet.setText(message);
        }


        if(routerActivity != null && !sms.routingUrls.isEmpty()) {
            holder.state.setText(sms.getRouterStatus());
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

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedItems.getValue() != null && !selectedItems.getValue().isEmpty()) {
                    List<String> items = selectedItems.getValue();
                    if(selectedItems.getValue().contains(holder.id)) {
                        items.remove(holder.id);
                        holder.unHighlight();
                    }
                    else {
                        items = selectedItems.getValue();
                        items.add(holder.id);
                        holder.highlight();
                    }
                    selectedItems.postValue(items);
                }
                else {
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
            }
        };

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                List<String> items = new ArrayList<>();
                if(selectedItems.getValue() != null && selectedItems.getValue().contains(holder.id)) {
                    items = selectedItems.getValue();
                    items.remove(holder.id);
                    holder.unHighlight();
                }
                else {
                    if(selectedItems.getValue() != null && !selectedItems.getValue().isEmpty()) {
                        items = selectedItems.getValue();
                    }
                    items.add(holder.id);
                    holder.highlight();
                }
                selectedItems.setValue(items);
                return true;
            }
        };

        holder.layout.setOnClickListener(onClickListener);
        holder.layout.setOnLongClickListener(onLongClickListener);
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
        public AvatarView contactInitials;
        public TextView youLabel;

        public ImageView contactPhoto;

        public boolean isContact = false;


        ConstraintLayout layout;

        final int recyclerViewTimeUpdateLimit = 60 * 1000;
        public ViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView);

            snippet = itemView.findViewById(R.id.messages_thread_text);
            address = itemView.findViewById(R.id.messages_thread_address_text);
            date = itemView.findViewById(R.id.messages_thread_date);
            layout = itemView.findViewById(R.id.messages_threads_layout);
            state = itemView.findViewById(R.id.messages_route_state);
            routingUrl = itemView.findViewById(R.id.message_route_url);
            routingURLText = itemView.findViewById(R.id.message_route_status);
            youLabel = itemView.findViewById(R.id.message_you_label);
            contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
            contactPhoto = itemView.findViewById(R.id.messages_threads_contact_photo);
            this.isContact = isContact;


//                final Random random = new Random();
//                final int red = random.nextInt(150) + 50;
//                final int green = random.nextInt(150) + 50;
//                final int blue = random.nextInt(150) + 50;
//
//                final int randomColor = Color.rgb(red, green, blue);
//
//                Drawable drawable = contactPhoto.getDrawable();
//                drawable.setColorFilter(randomColor, PorterDuff.Mode.SRC_IN);
//                contactPhoto.setImageDrawable(drawable);
            if(isContact) {
                contactInitials.setVisibility(View.VISIBLE);
                contactPhoto.setVisibility(View.GONE);
//                this.setIsRecyclable(false);
            }
        }

        public void highlight(){
            layout.setBackgroundResource(R.drawable.received_messages_drawable);
            this.setIsRecyclable(false);
        }

        public void unHighlight(){
            layout.setBackgroundResource(0);
            this.setIsRecyclable(true);
        }

    }

    public static class EncryptedViewHolder extends ViewHolder {

        public EncryptedViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            snippet.setText(R.string.messages_thread_encrypted_content);
        }
    }

    public static class ContactsViewHolder extends ViewHolder {

        public ContactsViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);

            snippet.setMaxLines(1);
            address.setMaxLines(1);

            routingUrl.setVisibility(View.GONE);
            routingURLText.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
            state.setVisibility(View.GONE);
        }
    }

    public static class UnreadViewHolder extends ViewHolder {
        public UnreadViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            address.setTypeface(Typeface.DEFAULT_BOLD);
            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTypeface(Typeface.DEFAULT_BOLD);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class UnreadEncryptedViewHolder extends UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
            snippet.setText(R.string.messages_thread_encrypted_content);
        }
    }

    public static class SentViewHolder extends ViewHolder {
        public SentViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setMaxLines(1);
            youLabel.setVisibility(View.VISIBLE);
        }
    }
    public static class SentViewHolderUnread extends UnreadViewHolder {
        public SentViewHolderUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setMaxLines(1);
            youLabel.setVisibility(View.VISIBLE);
        }
    }

    public static class SentEncryptedViewHolderUnread extends SentViewHolderUnread {
        public SentEncryptedViewHolderUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setText(R.string.messages_thread_encrypted_content);
        }
    }

    public static class SentEncryptedViewHolder extends SentViewHolder {
        public SentEncryptedViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            snippet.setText(R.string.messages_thread_encrypted_content);
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
