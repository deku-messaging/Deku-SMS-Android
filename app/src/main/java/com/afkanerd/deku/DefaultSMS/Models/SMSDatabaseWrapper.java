package com.afkanerd.deku.DefaultSMS.Models;

import android.content.Context;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Base64;

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

    public static void send_text(Context context, Conversation conversation) throws Exception {
        String transmissionAddress = Helpers.getFormatForTransmission(conversation.getAddress(),
                Helpers.getUserCountry(context));
        String[] nativeOutputs = NativeSMSDB.Outgoing._send_text(context, conversation.getMessage_id(),
                transmissionAddress, conversation.getText(), conversation.getSubscription_id(), null);

        conversation.setThread_id(nativeOutputs[NativeSMSDB.THREAD_ID]);
    }
}
