package com.example.swob_deku.Models.Archive;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value={"messageId"}, unique = true)})
public class Archive {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String messageId;
    public String threadId;

    public Archive(String messageId, String threadId) {
        this.messageId = messageId;
        this.threadId = threadId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
}
