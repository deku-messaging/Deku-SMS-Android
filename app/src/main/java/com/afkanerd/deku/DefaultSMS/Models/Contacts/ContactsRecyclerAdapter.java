package com.afkanerd.deku.DefaultSMS.Models.Contacts;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.TemplateViewHolder;
import com.afkanerd.deku.DefaultSMS.ConversationActivity;
import com.afkanerd.deku.DefaultSMS.R;

import java.util.List;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter{

    Context context;

    String sharedMessage;
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

        viewHolder.bind(contacts, sharedMessage);
    }

    public void setSharedMessage(String sharedMessage) {
        this.sharedMessage = sharedMessage;
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<Contacts> contactsList) {
        mDiffer.submitList(contactsList);
    }

    public static class ContactsViewHolder extends TemplateViewHolder {
        View itemView;
        public ContactsViewHolder(@NonNull View itemView, boolean isContact) {
            super(itemView);

            snippet.setMaxLines(1);
            address.setMaxLines(1);
            date.setVisibility(View.GONE);

            this.itemView = itemView;
        }

        public void bind(Contacts contacts, final String sharedConversation) {
            final int color = Helpers.generateColor(contacts.contactName);
            address.setText(contacts.contactName);
            contactInitials.setAvatarInitials(contacts.contactName.substring(0, 1));
            contactInitials.setAvatarInitialsBackgroundColor(color);
            snippet.setText(contacts.number);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent singleMessageThreadIntent = new Intent(itemView.getContext(), ConversationActivity.class);
                    singleMessageThreadIntent.putExtra(Conversation.ADDRESS, contacts.number);

                    if(sharedConversation != null && !sharedConversation.isEmpty())
                        singleMessageThreadIntent.putExtra(Conversation.SHARED_SMS_BODY, sharedConversation);

                    itemView.getContext().startActivity(singleMessageThreadIntent);
                }
            });
        }
    }
}
