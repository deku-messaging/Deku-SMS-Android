package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.provider.Telephony;

import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;

import java.util.List;

public class ConversationHandler {

    public static ConversationDao conversationDao;

    public static Datastore databaseConnector;
    public static Datastore acquireDatabase(Context context) {
        if(databaseConnector == null || !databaseConnector.isOpen())
            databaseConnector = Room.databaseBuilder(context, Datastore.class,
                            Datastore.databaseName)
                    .enableMultiInstanceInvalidation()
                    .build();
        return databaseConnector;
    }

    public static Conversation buildConversationForSending(Context context, String body, int subscriptionId,
                                                           String address) {
        long threadId = Telephony.Threads.getOrCreateThreadId(context, address);
        Conversation conversation = new Conversation();
        conversation.setMessage_id(String.valueOf(System.currentTimeMillis()));
        conversation.setText(body);
        conversation.setSubscription_id(subscriptionId);
        conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
        conversation.setDate(String.valueOf(System.currentTimeMillis()));
        conversation.setAddress(address);
        conversation.setThread_id(String.valueOf(threadId));
        conversation.setStatus(Telephony.Sms.STATUS_PENDING);
        return conversation;
    }

}
