package com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class Conversations {
    public int MESSAGE_COUNT;
    public String SNIPPET;
    public String THREAD_ID;

    public String getMESSAGE_ID() {
        return MESSAGE_ID;
    }

    public void setMESSAGE_ID(String MESSAGE_ID) {
        this.MESSAGE_ID = MESSAGE_ID;
    }

    public String MESSAGE_ID;
    public String ADDRESS;

    private SMSMetaEntity smsMetaEntity;

    public Conversations(Cursor cursor) {
        int snippetIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);
        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID);
        int msgCountIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.MESSAGE_COUNT);

        this.SNIPPET = cursor.getString(snippetIndex);
        this.THREAD_ID = cursor.getString(threadIdIndex);
        this.MESSAGE_COUNT = cursor.getInt(msgCountIndex);
    }

    public Conversations(){}

    public void setNewestMessage(Context context) {
        this.smsMetaEntity = new SMSMetaEntity();
        this.smsMetaEntity.setThreadId(context, this.THREAD_ID);
    }

    public void setSNIPPET(String snippet) {
        this.SNIPPET = snippet;
    }

    public void setTHREAD_ID(String threadId) {
        this.THREAD_ID = threadId;
    }

    public SMSMetaEntity getNewestMessage() {
        return this.smsMetaEntity;
    }

    public static final DiffUtil.ItemCallback<Conversations> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversations>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversations oldItem, @NonNull Conversations newItem) {
            return oldItem.THREAD_ID.equals(newItem.THREAD_ID);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversations oldItem, @NonNull Conversations newItem) {
            return oldItem.equals(newItem);
        }
    };

    public boolean equals(Conversations conv1, Conversations conv2) {
        return conv1.getMESSAGE_ID().equals(conv2.getMESSAGE_ID()) &&
                conv1.THREAD_ID.equals(conv2.THREAD_ID);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Conversations) {
            Conversations conversations = (Conversations) obj;
            if(this.smsMetaEntity != null && conversations.smsMetaEntity != null)
                return conversations.THREAD_ID.equals(this.THREAD_ID) &&
                        conversations.SNIPPET.equals(this.SNIPPET) &&
                        conversations.getNewestMessage().getNewestIsRead() == this.smsMetaEntity.getNewestIsRead();

            if(conversations.SNIPPET != null) {
                return conversations.THREAD_ID.equals(this.THREAD_ID) &&
                        conversations.SNIPPET.equals(this.SNIPPET);
            }

            if(conversations.MESSAGE_ID != null)  {
                return conversations.THREAD_ID.equals(this.THREAD_ID) &&
                        conversations.MESSAGE_ID.equals(this.MESSAGE_ID);
            }

        }
        return false;
    }
}
