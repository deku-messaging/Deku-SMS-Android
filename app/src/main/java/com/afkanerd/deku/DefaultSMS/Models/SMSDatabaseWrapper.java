package com.afkanerd.deku.DefaultSMS.Models;

import android.content.Context;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SMSDatabaseWrapper extends NativeSMSDB.Outgoing {

    public static void send_data(Context context, Conversation conversation) throws Exception {
        String transmissionAddress = Helpers.getFormatForTransmission(conversation.getAddress(),
                Helpers.getUserCountry(context));
        String[] nativeOutputs = NativeSMSDB.Outgoing._send_data(context, conversation.getMessage_id(),
                transmissionAddress, Base64.decode(conversation.getData(), Base64.DEFAULT),
                conversation.getSubscription_id(), null);
        if(nativeOutputs == null) {
            return;
        }

        conversation.setThread_id(nativeOutputs[NativeSMSDB.THREAD_ID]);
    }

    public static void send_text(Context context, Conversation conversation, Bundle bundle) throws Exception {
        String transmissionAddress = Helpers.getFormatForTransmission(conversation.getAddress(),
                Helpers.getUserCountry(context));
        String[] nativeOutputs = NativeSMSDB.Outgoing._send_text(context, conversation.getMessage_id(),
                transmissionAddress, conversation.getText(),
                conversation.getSubscription_id(), bundle);

//        conversation.setThread_id(nativeOutputs[NativeSMSDB.THREAD_ID]);
    }

    public static void saveDraft(Context context, Conversation conversation) {
        Log.d(SMSDatabaseWrapper.class.getName(), "Saving draft: " + conversation.getText());
        String[] outputs = NativeSMSDB.Outgoing.register_drafts(context, conversation.getMessage_id(),
                conversation.getAddress(), conversation.getText(), conversation.getSubscription_id());

//        return outputs[NativeSMSDB.THREAD_ID];
    }

    public static void deleteDraft(Context context, String threadId) {
        NativeSMSDB.deleteType(context, String.valueOf(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT),
                threadId);
    }
}
