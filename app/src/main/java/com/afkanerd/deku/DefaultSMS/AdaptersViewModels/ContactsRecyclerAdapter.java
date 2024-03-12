package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders.ThreadedConversationsTemplateViewHolder;
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
        View view = inflater.inflate(R.layout.conversations_threads_layout, parent, false);
        return new ContactsViewHolderThreadedConversations(view, true);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Contacts contacts = mDiffer.getCurrentList().get(holder.getAbsoluteAdapterPosition());
        ContactsViewHolderThreadedConversations viewHolder = (ContactsViewHolderThreadedConversations) holder;

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

    public static class ContactsViewHolderThreadedConversations extends ThreadedConversationsTemplateViewHolder {
        View itemView;
       public ContactsViewHolderThreadedConversations(@NonNull View itemView, boolean isContact) {
            super(itemView);

           this.itemView = itemView;
            snippet.setMaxLines(1);
            address.setMaxLines(1);
            date.setVisibility(View.GONE);

            contactAvatar.setVisibility(View.GONE);
        }

        public void bind(Contacts contacts, final String sharedConversation) {
//            final int color = Helpers.generateColor(contacts.contactName);
            int color = Helpers.getColor(itemView.getContext(), contacts.number);
            address.setText(contacts.contactName);
            contactInitials.setAvatarInitials(contacts.contactName.substring(0, 1));
            contactInitials.setAvatarInitialsBackgroundColor(color);
            snippet.setText(contacts.number);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent singleMessageThreadIntent = new Intent(itemView.getContext(),
                            ConversationActivity.class);
                    singleMessageThreadIntent.putExtra(Conversation.ADDRESS, contacts.number);

                    if(sharedConversation != null && !sharedConversation.isEmpty())
                        singleMessageThreadIntent.putExtra(Conversation.SHARED_SMS_BODY, sharedConversation);

                    itemView.getContext().startActivity(singleMessageThreadIntent);
                }
            });
        }
    }
}
