package com.example.swob_deku.Models.Messages.ViewHolders;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.R;

import io.getstream.avatarview.AvatarView;

public class TemplateViewHolder extends RecyclerView.ViewHolder {

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
    public ImageView encryptedLock;

    public boolean isContact = false;


    public ConstraintLayout layout;

    final int recyclerViewTimeUpdateLimit = 60 * 1000;
    public TemplateViewHolder(@NonNull View itemView) {
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
//        this.isContact = isContact;
//
//        if(isContact) {
//            contactInitials.setVisibility(View.VISIBLE);
//            contactPhoto.setVisibility(View.GONE);
//        }
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
            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class UnreadEncryptedViewHolder extends TemplateViewHolder.UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));

            encryptedLock = itemView.findViewById(R.id.messages_thread_secured_lock);
            encryptedLock.setVisibility(View.VISIBLE);
        }
    }

    public static class ReadEncryptedViewHolder extends TemplateViewHolder.ReadViewHolder {
        public ReadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

            encryptedLock = itemView.findViewById(R.id.messages_thread_secured_lock);
            encryptedLock.setVisibility(View.VISIBLE);
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
