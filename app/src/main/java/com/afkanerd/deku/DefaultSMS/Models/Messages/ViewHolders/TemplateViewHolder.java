package com.afkanerd.deku.DefaultSMS.Models.Messages.ViewHolders;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.card.MaterialCardView;

import io.getstream.avatarview.AvatarView;

public class TemplateViewHolder extends RecyclerView.ViewHolder {

    public String id;
    public long messageId;
    public TextView snippet;
    public TextView address;
    public TextView date;
    public AvatarView contactInitials;
    public TextView youLabel;

    public ConstraintLayout layout;

    public MaterialCardView materialCardView;

    public TemplateViewHolder(@NonNull View itemView) {
        super(itemView);

        snippet = itemView.findViewById(R.id.messages_thread_text);
        address = itemView.findViewById(R.id.messages_thread_address_text);
        date = itemView.findViewById(R.id.messages_thread_date);
        layout = itemView.findViewById(R.id.messages_threads_layout);
        youLabel = itemView.findViewById(R.id.message_you_label);
        contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
        materialCardView = itemView.findViewById(R.id.messages_threads_cardview);
    }

    public void init(Conversations conversation, View.OnClickListener onClickListener,
                     View.OnLongClickListener onLongClickListener) {
        this.id = conversation.THREAD_ID;
//
        final SMS.SMSMetaEntity smsMetaEntity = conversation.getNewestMessage();
        String address = smsMetaEntity.getAddress();
        if(smsMetaEntity.isContact()) {
            address = smsMetaEntity.getContactName();
            if(!address.isEmpty()) {
                this.contactInitials.setAvatarInitials(address.substring(0, 1));
                this.contactInitials.setAvatarInitialsBackgroundColor(Helpers.generateColor(address));
            }
        }
        this.address.setText(address);
        this.date.setText(smsMetaEntity.getFormattedDate());
        this.snippet.setText(conversation.SNIPPET);
        this.materialCardView.setOnClickListener(onClickListener);
        this.materialCardView.setOnLongClickListener(onLongClickListener);
    }

    public static class ReadViewHolder extends TemplateViewHolder{
        public ReadViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setMaxLines(1);
        }
    }

    public static class UnreadViewHolder extends TemplateViewHolder{
        public UnreadViewHolder(@NonNull View itemView) {
            super(itemView);
            address.setTypeface(Typeface.DEFAULT_BOLD);
            address.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            snippet.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            date.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));
        }
    }

    public static class UnreadEncryptedViewHolder extends TemplateViewHolder.UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        }
    }

    public static class ReadEncryptedViewHolder extends TemplateViewHolder.ReadViewHolder {
        public ReadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
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
