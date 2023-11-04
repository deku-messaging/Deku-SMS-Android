package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Fts3;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Migrations;

@Entity(indices = {@Index(value={"message_id"}, unique=true)})
public class Conversation {

    @Ignore
    public static String BROADCAST_THREAD_ID_INTENT = "BROADCAST_THREAD_ID_INTENT";

    @Ignore
    public static String BROADCAST_CONVERSATION_ID_INTENT = "BROADCAST_CONVERSATION_ID_INTENT";

    @PrimaryKey(autoGenerate = true)
    long id;
    long message_id;
    String thread_id;

    String date;
    String date_sent;

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

    public static ConversationDao getDao(Context context) {
        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration8To9())
                .build();
        ConversationDao conversationDao =  databaseConnector.conversationDao();
        databaseConnector.close();
        return conversationDao;
    }

    public String getDate_sent() {
        return date_sent;
    }

    public void setDate_sent(String date_sent) {
        this.date_sent = date_sent;
    }

    public String getMessage_id() {
        return String.valueOf(message_id);
    }

    public void setMessage_id(String message_id) {
        this.message_id = Long.parseLong(message_id);
    }

    public String getThread_id() {
        return thread_id;
    }

    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getNum_segments() {
        return num_segments;
    }

    public void setNum_segments(int num_segments) {
        this.num_segments = num_segments;
    }

    public int getSubscription_id() {
        return subscription_id;
    }

    public void setSubscription_id(int subscription_id) {
        this.subscription_id = subscription_id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isIs_encrypted() {
        return is_encrypted;
    }

    public void setIs_encrypted(boolean is_encrypted) {
        this.is_encrypted = is_encrypted;
    }

    public boolean isIs_key() {
        return is_key;
    }

    public void setIs_key(boolean is_key) {
        this.is_key = is_key;
    }

    public boolean isIs_image() {
        return is_image;
    }

    public void setIs_image(boolean is_image) {
        this.is_image = is_image;
    }

    public String getFormatted_date() {
        return formatted_date;
    }

    public void setFormatted_date(String formatted_date) {
        this.formatted_date = formatted_date;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static Conversation build(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
        int bodyIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY);
        int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
        int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
        int dateIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
        int dateSentIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE_SENT);
        int typeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE);
        int statusIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.STATUS);
        int readIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.READ);
        int subscriptionIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID);

        Conversation conversation = new Conversation();
        conversation.setMessage_id(cursor.getString(idIndex));
        conversation.setBody(cursor.getString(bodyIndex));
        conversation.setThread_id(cursor.getString(threadIdIndex));
        conversation.setAddress(cursor.getString(addressIndex));
        conversation.setDate(cursor.getString(dateIndex));
        conversation.setDate_sent(cursor.getString(dateSentIndex));
        conversation.setType(cursor.getInt(typeIndex));
        conversation.setStatus(cursor.getInt(statusIndex));
        conversation.setRead(cursor.getInt(readIndex) == 1);
        conversation.setSubscription_id(cursor.getInt(subscriptionIdIndex));

        return conversation;
    }


    public static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.message_id == newItem.message_id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.equals(newItem);
        }
    };

    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Conversation) {
            Conversation conversation = (Conversation) obj;
            return conversation.thread_id == this.thread_id &&
                    conversation.message_id == this.message_id &&
                    conversation.body.equals(this.body) &&
                    conversation.status == this.status &&
                    conversation.date == this.date &&
                    conversation.address.equals(this.address) &&
                    conversation.type == this.type;
        }
        return super.equals(obj);
    }
}
