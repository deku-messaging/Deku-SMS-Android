package com.example.swob_deku.Models.SMS;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class Conversations {
    public int MESSAGE_COUNT;
    public String SNIPPET;
    public String THREAD_ID;
    public String ADDRESS;

    private SMS.SMSMetaEntity smsMetaEntity;

    public Conversations(Cursor cursor) {
        int snippetIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);
        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID);
        int msgCountIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.MESSAGE_COUNT);

        this.SNIPPET = cursor.getString(snippetIndex);
        this.THREAD_ID = cursor.getString(threadIdIndex);
        this.MESSAGE_COUNT = cursor.getInt(msgCountIndex);
    }

    public void setNewestMessage(Context context) {
        this.smsMetaEntity = new SMS.SMSMetaEntity();
        this.smsMetaEntity.setThreadId(context, this.THREAD_ID);
    }

    public SMS.SMSMetaEntity getNewestMessage() {
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Conversations) {
            Conversations conversations = (Conversations) obj;
            return conversations.THREAD_ID.equals(this.THREAD_ID) &&
                            conversations.SNIPPET.equals(this.SNIPPET);

        }
        return false;
    }
}
