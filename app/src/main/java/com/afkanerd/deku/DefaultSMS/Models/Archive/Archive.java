package com.afkanerd.deku.DefaultSMS.Models.Archive;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value={"threadId"}, unique = true)})
public class Archive {
    @PrimaryKey
    public long threadId;

    public Archive(long threadId) {
        this.threadId = threadId;
    }


    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }
}
