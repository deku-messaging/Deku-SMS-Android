package com.afkanerd.deku.Router.Router;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

public class RouterItem extends Conversation  {

    public String url;
    public String  tag;
    public String MSISDN;
    public long routingDate;

    public String routingStatus;

    public String sid;

    public String reportedStatus;

    public RouterItem(Cursor cursor) {
        super(cursor);
        this.MSISDN = this.getAddress();
        this.text = this.getText();
    }

    public RouterItem() {

    }

    public static RouterItem build(Cursor cursor) {
        return (RouterItem) Conversation.build(cursor);
    }

    public static final DiffUtil.ItemCallback<RouterItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<RouterItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouterItem oldItem, @NonNull RouterItem newItem) {
            return oldItem.getMessage_id().equals(newItem.getMessage_id());
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouterItem oldItem, @NonNull RouterItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof RouterItem) {
            RouterItem routerItem = (RouterItem) obj;

            return routerItem.getMessage_id().equals(this.getMessage_id()) &&
                    routerItem.getText().equals(this.getText()) &&
                    routerItem.url.equals(this.url) &&
                    routerItem.routingDate == this.routingDate;
        }
        return false;
    }

}
