package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;

@Entity(indices = {@Index(value={"message_id"}, unique=true)})
public class Conversation {

    @Ignore
    public static String BROADCAST_THREAD_ID_INTENT = "BROADCAST_THREAD_ID_INTENT";

    @Ignore
    public static String BROADCAST_CONVERSATION_ID_INTENT = "BROADCAST_CONVERSATION_ID_INTENT";
    @Ignore
    public static final String ID = "ID";
    public static final String ADDRESS = "ADDRESS";
    public static final String THREAD_ID = "THREAD_ID";
    public static final String SHARED_SMS_BODY = "SHARED_SMS_BODY";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String message_id;
    public String thread_id;

    public String date;
    public String date_sent;

    public int type;
    public int num_segments;

    public int subscription_id;

    public int status;

    public int error_code;

    public boolean read;

    public boolean is_encrypted;

    public boolean is_key;
    public boolean is_image;
    public String formatted_date;

    public String address;

    public String text;

    public String data;


    public static ConversationDao getDao(Context context) {
        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration8To9())
                .build();
        ConversationDao conversationDao =  databaseConnector.conversationDao();
        databaseConnector.close();
        return conversationDao;
    }

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
        this.message_id = message_id;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Conversation(){}

    public Conversation(Cursor cursor) {
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

        this.setMessage_id(cursor.getString(idIndex));
        this.setText(cursor.getString(bodyIndex));
        this.setThread_id(cursor.getString(threadIdIndex));
        this.setAddress(cursor.getString(addressIndex));
        this.setDate(cursor.getString(dateIndex));
        this.setDate_sent(cursor.getString(dateSentIndex));
        this.setType(cursor.getInt(typeIndex));
        this.setStatus(cursor.getInt(statusIndex));
        this.setRead(cursor.getInt(readIndex) == 1);
        this.setSubscription_id(cursor.getInt(subscriptionIdIndex));
    }

    public static Conversation buildForDataTransmission(Conversation conversation, byte[] transmissionData) {
        String messageId = String.valueOf(System.currentTimeMillis());
        Conversation newConversation = new Conversation();
        newConversation.setIs_key(conversation.isIs_key());
        newConversation.setMessage_id(messageId);
        newConversation.setData(Base64.encodeToString(transmissionData, Base64.DEFAULT));
        newConversation.setSubscription_id(conversation.getSubscription_id());
        newConversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
        newConversation.setDate(String.valueOf(System.currentTimeMillis()));
        newConversation.setAddress(conversation.getAddress());
        newConversation.setStatus(Telephony.Sms.STATUS_PENDING);

        return newConversation;
    }

    public static Conversation build(Cursor cursor) {
        return new Conversation(cursor);
    }


    public static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.message_id.equals(newItem.message_id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.equals(newItem);
        }
    };

    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Conversation) {
            Conversation conversation = (Conversation) obj;
            if(data == null && text == null)
                return false;

            if(data ==  null)
                return conversation.thread_id.equals(this.thread_id) &&
                        conversation.message_id.equals(this.message_id) &&
                        conversation.text.equals(this.text) &&
                        conversation.status == this.status &&
                        conversation.date.equals(this.date) &&
                        conversation.address.equals(this.address) &&
                        conversation.isRead() == this.isRead() &&
                        conversation.type == this.type;
            if(text == null)
                return conversation.thread_id.equals(this.thread_id) &&
                        conversation.message_id.equals(this.message_id) &&
                        conversation.data.equals(this.data) &&
                        conversation.status == this.status &&
                        conversation.date.equals(this.date) &&
                        conversation.address.equals(this.address) &&
                        conversation.isRead() == this.isRead() &&
                        conversation.type == this.type;
        }
        return super.equals(obj);
    }
}
