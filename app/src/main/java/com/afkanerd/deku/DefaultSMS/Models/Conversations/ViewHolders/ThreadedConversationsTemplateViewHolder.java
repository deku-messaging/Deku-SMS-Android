package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import static android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

import static com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter.RECEIVED_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter.RECEIVED_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter.SENT_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter.SENT_VIEW_TYPE;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import io.getstream.avatarview.AvatarView;

public class ThreadedConversationsTemplateViewHolder extends RecyclerView.ViewHolder {

    public String id;
    public long messageId;
    public TextView snippet;
    public TextView address;
    public TextView date;
    public AvatarView contactInitials;
    public ImageView contactAvatar;
    public ImageView muteAvatar;
    public TextView youLabel;

    public ConstraintLayout layout;

    public MaterialCardView materialCardView;

    public View itemView;

    public ThreadedConversationsTemplateViewHolder(@NonNull View itemView) {
        super(itemView);
        this.itemView = itemView;

        snippet = itemView.findViewById(R.id.messages_thread_text);
        address = itemView.findViewById(R.id.messages_thread_address_text);
        date = itemView.findViewById(R.id.messages_thread_date);
        layout = itemView.findViewById(R.id.messages_threads_layout);
        youLabel = itemView.findViewById(R.id.message_you_label);
        contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
        materialCardView = itemView.findViewById(R.id.messages_threads_cardview);
        contactAvatar = itemView.findViewById(R.id.messages_threads_contact_photo);
        muteAvatar = itemView.findViewById(R.id.messages_threads_mute_icon);
    }

    public void bind(ThreadedConversations conversation, View.OnClickListener onClickListener,
                     View.OnLongClickListener onLongClickListener, String defaultRegion) {
        this.id = String.valueOf(conversation.getThread_id());

        int contactColor = Helpers.getColor(itemView.getContext(), id);
        if(conversation.getContact_name() != null && !conversation.getContact_name().isEmpty()) {
            this.contactAvatar.setVisibility(View.GONE);
            this.contactInitials.setVisibility(View.VISIBLE);
            this.contactInitials.setAvatarInitials(conversation.getContact_name().contains(" ") ?
                    conversation.getContact_name() : conversation.getContact_name().substring(0, 1));
            this.contactInitials.setAvatarInitialsBackgroundColor(contactColor);
        }
        else {
            this.contactAvatar.setVisibility(View.VISIBLE);
            this.contactInitials.setVisibility(View.GONE);
            Drawable drawable = contactAvatar.getDrawable();
            if (drawable == null) {
                drawable = itemView.getContext().getDrawable(R.drawable.baseline_account_circle_24);
            }
            if(drawable != null)
                drawable.setColorFilter(contactColor, PorterDuff.Mode.SRC_IN);
            contactAvatar.setImageDrawable(drawable);
        }
        if(conversation.getContact_name() != null) {
            this.address.setText(conversation.getContact_name());
        }
        else this.address.setText(conversation.getAddress());

        this.snippet.setText(conversation.getSnippet());
        String date = Helpers.formatDate(itemView.getContext(),
                Long.parseLong(conversation.getDate()));
        this.date.setText(date);
        this.materialCardView.setOnClickListener(onClickListener);
        this.materialCardView.setOnLongClickListener(onLongClickListener);

        String e16Address = Helpers.getFormatCompleteNumber(conversation.getAddress(), defaultRegion);
        if(Contacts.isMuted(itemView.getContext(), e16Address) ||
                Contacts.isMuted(itemView.getContext(), conversation.getAddress()))
            this.muteAvatar.setVisibility(View.VISIBLE);
        else
            this.muteAvatar.setVisibility(View.GONE);

        // TODO: investigate new Avatar first before anything else
//        this.contactInitials.setPlaceholder(itemView.getContext().getDrawable(R.drawable.round_person_24));
    }

    public static int getViewType(int position, List<ThreadedConversations> items) {
        if(position >= items.size())
            return RECEIVED_VIEW_TYPE;
        ThreadedConversations threadedConversations = items.get(position);
        String snippet = threadedConversations.getSnippet();
        int type = threadedConversations.getType();

//        if(EncryptionHandlers.containersWaterMark(snippet) || EncryptionHandlers.isKeyExchange(snippet)) {
//            if(!threadedConversations.isIs_read()) {
//                return type == MESSAGE_TYPE_INBOX ?
//                        RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE : SENT_ENCRYPTED_UNREAD_VIEW_TYPE;
//            }
//            else {
//                return type == MESSAGE_TYPE_INBOX ?
//                        RECEIVED_ENCRYPTED_VIEW_TYPE : SENT_ENCRYPTED_VIEW_TYPE;
//            }
//        }
        if(!threadedConversations.isIs_read()) {
            return type == MESSAGE_TYPE_INBOX ?
                    RECEIVED_UNREAD_VIEW_TYPE : SENT_UNREAD_VIEW_TYPE;
        }
        else {
            return type == MESSAGE_TYPE_INBOX ?
                    RECEIVED_VIEW_TYPE : SENT_VIEW_TYPE;
        }
    }

    public void setCardOnClickListener(View.OnClickListener onClickListener) {
        this.materialCardView.setOnClickListener(onClickListener);
    }

    public static class ReadViewHolderThreadedConversations extends ThreadedConversationsTemplateViewHolder {
        public ReadViewHolderThreadedConversations(@NonNull View itemView) {
            super(itemView);
            snippet.setMaxLines(1);
        }
    }

    public static class UnreadViewHolderThreadedConversations extends ThreadedConversationsTemplateViewHolder {
        public UnreadViewHolderThreadedConversations(@NonNull View itemView) {
            super(itemView);
            address.setTextAppearance(R.style.conversation_unread_style);

            snippet.setTextAppearance(R.style.conversation_unread_style);

            date.setTextAppearance(R.style.conversation_unread_style);
        }
    }

    public static class UnreadEncryptedViewHolderThreadedConversations extends UnreadViewHolderThreadedConversations {
        public UnreadEncryptedViewHolderThreadedConversations(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
        }
    }

    public static class ReadEncryptedViewHolderThreadedConversations extends ReadViewHolderThreadedConversations {
        public ReadEncryptedViewHolderThreadedConversations(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
        }
    }

    public void highlight(){
        materialCardView.setBackgroundResource(R.drawable.received_messages_drawable);
//        this.setIsRecyclable(false);
    }

    public void unHighlight(){
        materialCardView.setBackgroundResource(0);
//        this.setIsRecyclable(true);
    }

}
