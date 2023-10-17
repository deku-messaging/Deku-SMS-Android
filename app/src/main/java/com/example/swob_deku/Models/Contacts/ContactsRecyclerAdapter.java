package com.example.swob_deku.Models.Contacts;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.ViewHolders.TemplateViewHolder;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;

import java.util.List;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter{

    Context context;

    String sharedSMS;
    private final AsyncListDiffer<Contacts> mDiffer = new AsyncListDiffer(this, Contacts.DIFF_CALLBACK);

    public ContactsRecyclerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.messages_threads_layout, parent, false);
        return new ContactsViewHolder(view, true);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Contacts contacts = mDiffer.getCurrentList().get(holder.getAbsoluteAdapterPosition());
        ContactsViewHolder viewHolder = (ContactsViewHolder) holder;

        final int color = Helpers.generateColor(contacts.contactName);
        viewHolder.address.setText(contacts.contactName);
        viewHolder.contactInitials.setAvatarInitials(contacts.contactName.substring(0, 1));
        viewHolder.contactInitials.setAvatarInitialsBackgroundColor(color);
        viewHolder.snippet.setText(contacts.number);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent singleMessageThreadIntent = new Intent(context, SMSSendActivity.class);
                singleMessageThreadIntent.putExtra(SMS.SMSMetaEntity.ADDRESS, contacts.number);

                if(sharedSMS != null && !sharedSMS.isEmpty())
                    singleMessageThreadIntent.putExtra(SMS.SMSMetaEntity.SHARED_SMS_BODY, sharedSMS);

                context.startActivity(singleMessageThreadIntent);
            }
        });
    }

    public void setSharedSMS(String sharedSMS) {
        this.sharedSMS = sharedSMS;
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<Contacts> contactsList) {
        mDiffer.submitList(contactsList);
    }

    public static class ContactsViewHolder extends TemplateViewHolder {
        public ContactsViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView);

            snippet.setMaxLines(1);
            address.setMaxLines(1);
            date.setVisibility(View.GONE);
        }
    }
}
