package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.graphics.Typeface;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.R;

public class ThreadedConversationsSentViewHandler {
    public static class SentViewHolderReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadViewHolderThreadedConversations {
        public SentViewHolderReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }

        @Override
        public void bind(ThreadedConversations conversation, View.OnClickListener onClickListener,
                         View.OnLongClickListener onLongClickListener, String defaultRegion) {
            super.bind(conversation, onClickListener, onLongClickListener, defaultRegion);
            if(conversation.getType() == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT) {
                this.date.setText(itemView.getContext().getString(R.string.thread_conversation_type_draft));
                this.date.setTextAppearance(R.style.conversation_draft_style);
                this.snippet.setTextAppearance(R.style.conversation_draft_style);
                this.youLabel.setTextAppearance(R.style.conversation_draft_style);
            }
        }
    }

    public static class SentViewHolderUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadViewHolderThreadedConversations {
        public SentViewHolderUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            this.youLabel.setTextAppearance(R.style.conversation_unread_style);
        }

        @Override
        public void bind(ThreadedConversations conversation, View.OnClickListener onClickListener,
                         View.OnLongClickListener onLongClickListener, String defaultRegion) {
            super.bind(conversation, onClickListener, onLongClickListener, defaultRegion);
            if(conversation.getType() == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT) {
                this.date.setText(itemView.getContext().getString(R.string.thread_conversation_type_draft));
                this.date.setTextAppearance(R.style.conversation_draft_style);
                this.snippet.setTextAppearance(R.style.conversation_draft_style);
                this.youLabel.setTextAppearance(R.style.conversation_draft_style);
            }
        }
    }

    public static class SentViewHolderEncryptedUnreadThreadedConversations extends ThreadedConversationsTemplateViewHolder.UnreadEncryptedViewHolderThreadedConversations {
        public SentViewHolderEncryptedUnreadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
            this.youLabel.setTextAppearance(R.style.conversation_unread_style);
        }
    }

    public static class SentViewHolderEncryptedReadThreadedConversations extends ThreadedConversationsTemplateViewHolder.ReadEncryptedViewHolderThreadedConversations {
        public SentViewHolderEncryptedReadThreadedConversations(@NonNull View itemView) {
            super(itemView);
            youLabel.setVisibility(View.VISIBLE);
        }
    }
}
