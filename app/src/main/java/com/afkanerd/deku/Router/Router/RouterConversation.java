package com.afkanerd.deku.Router.Router;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

public class RouterConversation extends Conversation  {

    public String url;
    public String  tag;
    public String text;
    public String MSISDN;
    public long routingDate;

    public String routingStatus;

    public RouterConversation(Cursor cursor) {
        super(cursor);
        this.MSISDN = this.getAddress();
        this.text = this.getBody();
    }

    public static RouterConversation build(Cursor cursor) {
        return (RouterConversation) Conversation.build(cursor);
    }

    public static final DiffUtil.ItemCallback<RouterConversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<RouterConversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouterConversation oldItem, @NonNull RouterConversation newItem) {
            return oldItem.getMessage_id().equals(newItem.getMessage_id());
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouterConversation oldItem, @NonNull RouterConversation newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof RouterConversation) {
            RouterConversation routerConversation = (RouterConversation) obj;

            return routerConversation.getMessage_id().equals(this.getMessage_id()) &&
                    routerConversation.getBody().equals(this.getBody()) &&
                    routerConversation.url.equals(this.url) &&
                    routerConversation.routingDate == this.routingDate;
        }
        return false;
    }

}
