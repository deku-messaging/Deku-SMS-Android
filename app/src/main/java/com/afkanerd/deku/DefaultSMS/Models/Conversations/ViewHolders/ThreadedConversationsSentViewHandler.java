package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

public class ThreadedConversationsSentViewHandler {
    public static class SentViewHolderReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadViewHolderThreadedConversations {
        public SentViewHolderReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }
    }

    public static class SentViewHolderUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadViewHolderThreadedConversations {
        public SentViewHolderUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadEncryptedViewHolderThreadedConversations {
        public SentViewHolderEncryptedUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            youLabel.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public static class SentViewHolderEncryptedReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadEncryptedViewHolderThreadedConversations {
        public SentViewHolderEncryptedReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }
    }
}
