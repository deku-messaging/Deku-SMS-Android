package com.afkanerd.deku.DefaultSMS.Models;

import android.content.Context;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

public class SMSDatabaseWrapper extends NativeSMSDB.Outgoing {

    public static void send_data(Context context, Conversation conversation) throws Exception {
        String transmissionAddress = Helpers.getFormatForTransmission(conversation.getAddress(),
                Helpers.getUserCountry(context));
        String[] nativeOutputs = NativeSMSDB.Outgoing._send_data(context, conversation.getMessage_id(),
                transmissionAddress, Base64.decode(conversation.getData(), Base64.DEFAULT),
                conversation.getSubscription_id(), null);
    }

    public static void send_text(Context context, Conversation conversation, Bundle bundle) throws Exception {
        String transmissionAddress = Helpers.getFormatForTransmission(conversation.getAddress(),
                Helpers.getUserCountry(context));
        String[] nativeOutputs = NativeSMSDB.Outgoing._send_text(context, conversation.getMessage_id(),
                transmissionAddress, conversation.getText(),
                conversation.getSubscription_id(), bundle);
    }

    public static void saveDraft(Context context, Conversation conversation) {
        Log.d(SMSDatabaseWrapper.class.getName(), "Saving draft: " + conversation.getText());
        String[] outputs = NativeSMSDB.Outgoing.register_drafts(context, conversation.getMessage_id(),
                conversation.getAddress(), conversation.getText(), conversation.getSubscription_id());
    }

    public static void deleteDraft(Context context, String threadId) {
        NativeSMSDB.deleteTypeForThread(context,
                String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT), threadId);
    }

    public static void deleteAllDraft(Context context) {
        NativeSMSDB.deleteAllType(context,
                String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT));
    }
}
