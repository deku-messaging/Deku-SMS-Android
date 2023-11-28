package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.view.View;

import androidx.annotation.NonNull;

public class ThreadedConversationsReceivedViewHandler {
    public static class ReceivedViewHolderReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadViewHolderThreadedConversations {
        public ReceivedViewHolderReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadViewHolderThreadedConversations {
        public ReceivedViewHolderUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderEncryptedUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadEncryptedViewHolderThreadedConversations {
        public ReceivedViewHolderEncryptedUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class ReceivedViewHolderEncryptedReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadEncryptedViewHolderThreadedConversations {
        public ReceivedViewHolderEncryptedReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
        }
    }
}
