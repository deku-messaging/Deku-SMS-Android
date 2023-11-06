package com.afkanerd.deku.Router.Router;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.List;

public class RouterRecyclerAdapter extends RecyclerView.Adapter<RouterRecyclerAdapter.ViewHolder> {
    private final AsyncListDiffer<RouterConversation> mDiffer = new AsyncListDiffer<>(this, RouterConversation.DIFF_CALLBACK);

    Context context;
    public RouterRecyclerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);

        View view = inflater.inflate(R.layout.routed_messages_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouterConversation routerConversation = mDiffer.getCurrentList().get(position);
        holder.init(routerConversation);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<RouterConversation> list) {
        mDiffer.submitList(list);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView address, url, body, status, date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.address = itemView.findViewById(R.id.routed_messages_address);
            this.url = itemView.findViewById(R.id.routed_messages_url);
            this.body = itemView.findViewById(R.id.routed_messages_body);
            this.status = itemView.findViewById(R.id.routed_messages_status);
            this.date = itemView.findViewById(R.id.routed_messages_date);
        }

        public void init(RouterConversation routerConversation) {
            this.address.setText(routerConversation.getAddress());
            this.url.setText(routerConversation.url);
            this.body.setText(routerConversation.getBody());
            this.status.setText(routerConversation.routingStatus);
            this.date.setText(Helpers.formatDate(itemView.getContext(),
                    routerConversation.routingDate));
        }
    }

}
