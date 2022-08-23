package com.example.swob_server.Models;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_server.Commons.Contacts;
import com.example.swob_server.R;
import com.example.swob_server.SendSMSActivity;


import java.util.List;

public class MessagesThreadRecyclerAdapter extends RecyclerView.Adapter<MessagesThreadRecyclerAdapter.ViewHolder> {

    Context context;
    List<SMS> messagesThreadList;
    int renderLayout;

    public MessagesThreadRecyclerAdapter(Context context, List<SMS> messagesThreadList, int renderLayout) {
       this.context = context;
       this.messagesThreadList = messagesThreadList;
       this.renderLayout = renderLayout;
    }

    @NonNull
    @Override
    public MessagesThreadRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(this.renderLayout, parent, false);
        return new MessagesThreadRecyclerAdapter.ViewHolder(view);
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.snippet.setText(messagesThreadList.get(position).getBody());

        String address = messagesThreadList.get(position).getAddress();

        if(checkPermissionToReadContacts())
            address = Contacts.retrieveContactName(context, address);

        holder.address.setText(address);

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent singleMessageThreadIntent = new Intent(context, SendSMSActivity.class);
                singleMessageThreadIntent.putExtra(SendSMSActivity.ADDRESS, messagesThreadList.get(position).getAddress());
                context.startActivity(singleMessageThreadIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesThreadList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView snippet;
        TextView address;

        ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            snippet = itemView.findViewById(R.id.messages_thread_text);
            address = itemView.findViewById(R.id.messages_thread_address_text);
            layout = itemView.findViewById(R.id.messages_threads_layout);
        }
    }
}
