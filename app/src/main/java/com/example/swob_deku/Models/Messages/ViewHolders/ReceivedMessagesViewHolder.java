package com.example.swob_deku.Models.Messages.ViewHolders;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

public class ReceivedMessagesViewHolder {
    public static class ReceivedViewHolderRead extends TemplateViewHolder.ReadViewHolder {
        public ReceivedViewHolderRead(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
        }
    }

    public static class ReceivedViewHolderUnread extends TemplateViewHolder.UnreadViewHolder {
        public ReceivedViewHolderUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
        }
    }

    public static class ReceivedViewHolderEncryptedUnread extends TemplateViewHolder.UnreadEncryptedViewHolder {
        public ReceivedViewHolderEncryptedUnread(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
        }
    }

    public static class ReceivedViewHolderEncryptedRead extends TemplateViewHolder.ReadEncryptedViewHolder {
        public ReceivedViewHolderEncryptedRead(@NonNull View itemView, boolean isContact) {
            super(itemView, isContact);
        }
    }
}
