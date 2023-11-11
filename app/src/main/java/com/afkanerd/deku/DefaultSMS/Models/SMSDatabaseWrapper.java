package com.afkanerd.deku.DefaultSMS.Models;

import android.content.Context;
import android.os.Bundle;
import android.provider.Telephony;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

public class SMSDatabaseWrapper extends NativeSMSDB.Outgoing {

    public static void send_text(Context context, String messageId, String destinationAddress,
                                 String text, int subscriptionId, Bundle bundle) {
        Conversation conversation = new Conversation();
        conversation.setMessage_id(messageId);
        conversation.setBody(text);
        conversation.setSubscription_id(subscriptionId);
        conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
        conversation.setDate(String.valueOf(System.currentTimeMillis()));
        conversation.setAddress(destinationAddress);
        conversation.setStatus(Telephony.Sms.STATUS_PENDING);
        ConversationDao conversationDao = Conversation.getDao(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                long id = conversationDao.insert(conversation);
                try {
                    String[] nativeOutputs = NativeSMSDB.Outgoing._send_text(context, messageId, destinationAddress,
                            text, subscriptionId, bundle);
                    Conversation conversation1 = new Conversation();
                    conversation1.setThread_id(nativeOutputs[NativeSMSDB.THREAD_ID]);
                    conversation1.setId(id);
                    conversation1.setMessage_id(messageId);
                    conversationDao.update(conversation1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
