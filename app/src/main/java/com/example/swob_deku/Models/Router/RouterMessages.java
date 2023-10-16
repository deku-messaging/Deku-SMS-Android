package com.example.swob_deku.Models.Router;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class RouterMessages {

    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    private String address;
    private String url;
    private String status;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    private long messageId;
    private String threadId;
    private long date;

    public static final DiffUtil.ItemCallback<RouterMessages> DIFF_CALLBACK = new DiffUtil.ItemCallback<RouterMessages>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouterMessages oldItem, @NonNull RouterMessages newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouterMessages oldItem, @NonNull RouterMessages newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof RouterMessages) {
            RouterMessages routerMessages = (RouterMessages) obj;

            return routerMessages.getMessageId() == this.getMessageId() &&
                    routerMessages.getBody().equals(this.getBody()) &&
                    routerMessages.getUrl().equals(this.getUrl()) &&
                    routerMessages.date == this.date;
        }
        return false;
    }

}
