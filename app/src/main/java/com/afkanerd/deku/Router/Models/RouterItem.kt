package com.afkanerd.deku.Router.Models;

import android.database.Cursor;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;

public class RouterItem extends Conversation  {
    public String routingUniqueId;

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
            return oldItem.first.routingUniqueId.equals(newItem.first.routingUniqueId);
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
            return super.equals(conversation) && this.routingStatus.equals(routerItem.routingStatus);
        }
        return false;
    }

}
