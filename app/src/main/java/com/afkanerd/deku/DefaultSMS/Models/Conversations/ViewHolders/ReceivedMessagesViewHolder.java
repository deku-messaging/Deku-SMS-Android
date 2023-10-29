package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.view.View;

import androidx.annotation.NonNull;

public class ReceivedMessagesViewHolder {
    public static class ReceivedViewHolderRead extends TemplateViewHolder.ReadViewHolder {
        public ReceivedViewHolderRead(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderUnread extends TemplateViewHolder.UnreadViewHolder {
        public ReceivedViewHolderUnread(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderEncryptedUnread extends TemplateViewHolder.UnreadEncryptedViewHolder {
        public ReceivedViewHolderEncryptedUnread(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderEncryptedRead extends TemplateViewHolder.ReadEncryptedViewHolder {
        public ReceivedViewHolderEncryptedRead(@NonNull View itemView) {
            super(itemView);
        }
    }
}
