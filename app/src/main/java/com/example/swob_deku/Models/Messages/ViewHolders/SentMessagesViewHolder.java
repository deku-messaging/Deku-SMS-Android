package com.example.swob_deku.Models.Messages.ViewHolders;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;

public class SentMessagesViewHolder {
    public static class SentViewHolderRead extends TemplateViewHolder.ReadViewHolder {
        public SentViewHolderRead(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            youLabel.setVisibility(View.VISIBLE);
        }
    }

    public static class SentViewHolderUnread extends TemplateViewHolder.UnreadViewHolder {
        public SentViewHolderUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedUnread extends TemplateViewHolder.UnreadEncryptedViewHolder {
        public SentViewHolderEncryptedUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedRead extends TemplateViewHolder.ReadEncryptedViewHolder {
        public SentViewHolderEncryptedRead(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
            youLabel.setVisibility(View.VISIBLE);
        }
    }
}
