package com.example.swob_deku.Models.Messages.ViewHolders;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.R;

import io.getstream.avatarview.AvatarView;

public class TemplateViewHolder extends RecyclerView.ViewHolder {

    public String id;
    public long messageId;
    public TextView snippet;
    public TextView address;
    public TextView date;
    public TextView state;
    public TextView routingUrl;
    public TextView routingURLText;
    public AvatarView contactInitials;
    public TextView youLabel;

    public ImageView contactPhoto;
    public ImageView encryptedLock;

    public ConstraintLayout layout;

    public FrameLayout contactsInitialsPhotoFrame;

    public TemplateViewHolder(@NonNull View itemView, boolean isContact) {
        super(itemView);

        snippet = itemView.findViewById(R.id.messages_thread_text);
        address = itemView.findViewById(R.id.messages_thread_address_text);
        date = itemView.findViewById(R.id.messages_thread_date);
        layout = itemView.findViewById(R.id.messages_threads_layout);
        state = itemView.findViewById(R.id.messages_route_state);
        routingUrl = itemView.findViewById(R.id.message_route_url);
        routingURLText = itemView.findViewById(R.id.message_route_status);
        youLabel = itemView.findViewById(R.id.message_you_label);
        contactsInitialsPhotoFrame = itemView.findViewById(R.id.messages_threads_contact_photo_id);
        contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
        contactPhoto = itemView.findViewById(R.id.messages_threads_contact_photo);
        encryptedLock = itemView.findViewById(R.id.messages_thread_secured_lock);

        if(isContact) {
            contactPhoto.setVisibility(View.GONE);
        }
        else {
            contactInitials.setVisibility(View.GONE);
        }
    }

    public static class ReadViewHolder extends TemplateViewHolder{
        public ReadViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setMaxLines(1);
        }
    }

    public static class UnreadViewHolder extends TemplateViewHolder{
        public UnreadViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            address.setTypeface(Typeface.DEFAULT_BOLD);
            address.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            snippet.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            date.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));
        }
    }

    public static class UnreadEncryptedViewHolder extends TemplateViewHolder.UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        }
    }

    public static class ReadEncryptedViewHolder extends TemplateViewHolder.ReadViewHolder {
        public ReadEncryptedViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
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
