package com.afkanerd.deku.Router.Router;

import android.database.Cursor;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.work.WorkInfo;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;

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

    public RouterItem(Conversation conversation) {
        super(conversation);
        this.MSISDN = this.getAddress();
        this.text = this.getText();
    }

    public static RouterItem build(Cursor cursor) {
        return (RouterItem) Conversation.build(cursor);
    }

    public static final DiffUtil.ItemCallback<Pair<RouterItem, GatewayServer>> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Pair<RouterItem, GatewayServer>>() {
        @Override
        public boolean areItemsTheSame(@NonNull Pair<RouterItem, GatewayServer> oldItem,
                                       @NonNull Pair<RouterItem, GatewayServer> newItem) {
            return oldItem.first.getMessage_id().equals(newItem.first.getMessage_id()) &&
                    oldItem.second.getId() == newItem.second.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Pair<RouterItem, GatewayServer> oldItem,
                                       @NonNull Pair<RouterItem, GatewayServer> newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof RouterItem) {
            Conversation conversation = (Conversation) obj;
            RouterItem routerItem = (RouterItem) obj;

//            return routerItem.getMessage_id().equals(this.getMessage_id()) &&
//                    routerItem.getText().equals(this.getText());
            return super.equals(conversation) && this.routingStatus.equals(routerItem.routingStatus);
        }
        return false;
    }

}
