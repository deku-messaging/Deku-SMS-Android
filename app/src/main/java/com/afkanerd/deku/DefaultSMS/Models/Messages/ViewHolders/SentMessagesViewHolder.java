package com.afkanerd.deku.DefaultSMS.Models.Messages.ViewHolders;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

public class SentMessagesViewHolder {
    public static class SentViewHolderRead extends TemplateViewHolder.ReadViewHolder {
        public SentViewHolderRead(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }
    }

    public static class SentViewHolderUnread extends TemplateViewHolder.UnreadViewHolder {
        public SentViewHolderUnread(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedUnread extends TemplateViewHolder.UnreadEncryptedViewHolder {
        public SentViewHolderEncryptedUnread(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedRead extends TemplateViewHolder.ReadEncryptedViewHolder {
        public SentViewHolderEncryptedRead(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }
    }
}
