package com.example.swob_deku.Models;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.R;
import com.example.swob_deku.SendSMSActivity;


import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class MessagesThreadRecyclerAdapter extends RecyclerView.Adapter<MessagesThreadRecyclerAdapter.ViewHolder> {

    Context context;
    List<SMS> messagesThreadList;
    int renderLayout;
    Boolean isSearch = false;
    String searchString = new String();

    public MessagesThreadRecyclerAdapter(Context context, List<SMS> messagesThreadList, int renderLayout) {
       this.context = context;
       this.messagesThreadList = messagesThreadList;
       this.renderLayout = renderLayout;
    }

    public MessagesThreadRecyclerAdapter(Context context, List<SMS> messagesThreadList, int renderLayout, Boolean isSearch, String searchString) {
        this.context = context;
        this.messagesThreadList = messagesThreadList;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
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
        SMS sms = messagesThreadList.get(position);

        if(isSearch && !searchString.isEmpty()) {

            Spannable spannable = Spannable.Factory.getInstance().newSpannable(sms.getBody());
            for(int index = sms.getBody().indexOf(searchString); index >=0; index = sms.getBody().indexOf(searchString, index + 1)) {
                spannable.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.highlight_yellow)),
                        index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.black)),
                        index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.snippet.setText(spannable);
        }
        else
            holder.snippet.setText(sms.getBody());

        holder.state.setText(sms.getRouterStatus());

        String address = sms.getAddress();
        String contactPhotoUri = Contacts.retrieveContactPhoto(context, address);

        if(checkPermissionToReadContacts())
            address = Contacts.retrieveContactName(context, address);

        if(!contactPhotoUri.isEmpty() && !contactPhotoUri.equals("null"))
            holder.contactPhoto.setImageURI(Uri.parse(contactPhotoUri));

        holder.address.setText(address);

        String date = sms.getDate();
        if (DateUtils.isToday(Long.parseLong(date))) {
            date = "Today";
        }
        else {
            DateFormat dateFormat = new SimpleDateFormat("MMM dd");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }
        holder.date.setText(date);

        if(SMSHandler.hasUnreadMessages(context, sms.getThreadId())) {
            // Make bold
            holder.address.setTypeface(null, Typeface.BOLD);
            holder.snippet.setTypeface(null, Typeface.BOLD);

            holder.address.setTextColor(context.getResources().getColor(R.color.white));
            holder.snippet.setTextColor(context.getResources().getColor(R.color.white));
        }


        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent singleMessageThreadIntent = new Intent(context, SendSMSActivity.class);
                singleMessageThreadIntent.putExtra(SendSMSActivity.ADDRESS, sms.getAddress());
                singleMessageThreadIntent.putExtra(SendSMSActivity.THREAD_ID, sms.getThreadId());

                if(isSearch)
                    singleMessageThreadIntent.putExtra(SendSMSActivity.ID, sms.getId());
                if(!searchString.isEmpty())
                    singleMessageThreadIntent.putExtra(SendSMSActivity.SEARCH_STRING, searchString);

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
        TextView date;
        TextView state;
        ImageView contactPhoto;

        ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            snippet = itemView.findViewById(R.id.messages_thread_text);
            address = itemView.findViewById(R.id.messages_thread_address_text);
            date = itemView.findViewById(R.id.messages_thread_date);
            layout = itemView.findViewById(R.id.messages_threads_layout);
            state = itemView.findViewById(R.id.messages_route_state);
            contactPhoto = itemView.findViewById(R.id.messages_threads_contact_photo);
        }
    }
}
