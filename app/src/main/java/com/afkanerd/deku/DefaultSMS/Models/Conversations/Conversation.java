package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Conversation {
    @PrimaryKey
    long message_id;
    long thread_id;

    long date;

    int type;
    int num_segments;

    int subscription_id;

    int status;

    boolean read;

    boolean is_encrypted;

    boolean is_key;

    boolean is_image;
    String formatted_date;

    String address;

    String body;

}
