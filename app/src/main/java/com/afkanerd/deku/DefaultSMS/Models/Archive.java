package com.afkanerd.deku.DefaultSMS.Models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Archive {
    @NonNull
    @PrimaryKey
    public String thread_id;

    public boolean is_archived;
}
