package com.example.swob_deku.Models.Router;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.R;

import java.util.List;

public class RouterRecyclerAdapter extends RecyclerView.Adapter<RouterRecyclerAdapter.ViewHolder> {
    private final AsyncListDiffer<RouterMessages> mDiffer = new AsyncListDiffer<>(this, RouterMessages.DIFF_CALLBACK);

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
        RouterMessages routerMessages = mDiffer.getCurrentList().get(position);
        holder.init(routerMessages);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<RouterMessages> list) {
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

        public void init(RouterMessages routerMessages) {
            this.address.setText(routerMessages.getAddress());
            this.url.setText(routerMessages.getUrl());
            this.body.setText(routerMessages.getBody());
            this.status.setText(routerMessages.getStatus());
            this.date.setText(Helpers.formatDate(itemView.getContext(), routerMessages.getDate()));
        }
    }

}
