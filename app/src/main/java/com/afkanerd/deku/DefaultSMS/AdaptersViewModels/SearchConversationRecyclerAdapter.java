package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;

import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.TemplateViewHolder;

public class SearchConversationRecyclerAdapter extends ThreadedConversationRecyclerAdapter {
    public final AsyncListDiffer<ThreadedConversations> mDiffer =
            new AsyncListDiffer(this, ThreadedConversations.DIFF_CALLBACK);
    public SearchConversationRecyclerAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return TemplateViewHolder.getViewType(position, mDiffer.getCurrentList());
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
//        super.onBindViewHolder(holder, position);
        ThreadedConversations threadedConversations = mDiffer.getCurrentList().get(position);
        String threadId = String.valueOf(threadedConversations.getThread_id());
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent singleMessageThreadIntent = new Intent(context, ConversationActivity.class);
                singleMessageThreadIntent.putExtra(Conversation.THREAD_ID, threadId);
                singleMessageThreadIntent.putExtra(ConversationActivity.SEARCH_STRING, searchString);
                singleMessageThreadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(singleMessageThreadIntent);
            }
        };
        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        };

        holder.bind(threadedConversations, onClickListener, onLongClickListener);
    }
}
