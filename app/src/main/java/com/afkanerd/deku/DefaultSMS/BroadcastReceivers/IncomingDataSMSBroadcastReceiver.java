package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.i18n.phonenumbers.NumberParseException;

//import org.bouncycastle.operator.OperatorCreationException;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncomingDataSMSBroadcastReceiver extends BroadcastReceiver {

    public static String DATA_DELIVER_ACTION = BuildConfig.APPLICATION_ID + ".DATA_DELIVER_ACTION" ;

    public static String DATA_SENT_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_SENT_BROADCAST_INTENT";

    public static String DATA_DELIVERED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_DELIVERED_BROADCAST_INTENT";

    public static String DATA_UPDATED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_UPDATED_BROADCAST_INTENT";

    Datastore databaseConnector;

    public ThreadedConversations insertThreads(Context context, Conversation conversation,
                                               boolean isSecure, boolean isSelf) {
        ThreadedConversations threadedConversations =
                databaseConnector.threadedConversationsDao().get(conversation.getThread_id());
        boolean available = threadedConversations != null;
        Log.d(getClass().getName(), "Threaded is null: " + String.valueOf(threadedConversations == null));
        if(!available) {
            threadedConversations = ThreadedConversations.build(context, conversation);
            String contactName = Contacts.retrieveContactName(context, conversation.getAddress());
            threadedConversations.setContact_name(contactName);
        }
        threadedConversations.setIs_secured(isSecure);
        threadedConversations.setSelf(isSelf);
        if(available)
            databaseConnector.threadedConversationsDao().update(threadedConversations);
        else
            databaseConnector.threadedConversationsDao().insert(threadedConversations);
        return threadedConversations;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * Important note: either image or dump it
         */

        if(Datastore.datastore == null || !Datastore.datastore.isOpen()) {
            Datastore.datastore = Room.databaseBuilder(context.getApplicationContext(),
                            Datastore.class, Datastore.databaseName)
                    .enableMultiInstanceInvalidation()
                    .build();
        }
        databaseConnector = Datastore.datastore;


        if (intent.getAction().equals(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                try {
                    String[] regIncomingOutput = NativeSMSDB.Incoming.register_incoming_data(context, intent);

                    final String threadId = regIncomingOutput[NativeSMSDB.THREAD_ID];
                    final String messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID];
                    final String data = regIncomingOutput[NativeSMSDB.BODY];
                    final String address = regIncomingOutput[NativeSMSDB.ADDRESS];
                    final String strSubscriptionId = regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID];
                    final String dateSent = regIncomingOutput[NativeSMSDB.DATE_SENT];
                    final String date = regIncomingOutput[NativeSMSDB.DATE];
                    int subscriptionId = Integer.parseInt(strSubscriptionId);

                    boolean isValidKey = E2EEHandler.isValidDefaultPublicKey( Base64.decode(data, Base64.DEFAULT));

                    Conversation conversation = new Conversation();
                    conversation.setData(data);
                    conversation.setAddress(address);
                    conversation.setIs_key(isValidKey);
                    conversation.setMessage_id(messageId);
                    conversation.setThread_id(threadId);
                    conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
                    conversation.setSubscription_id(subscriptionId);
                    conversation.setDate(dateSent);
                    conversation.setDate(date);

                    ThreadingPoolExecutor.executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            databaseConnector.conversationDao().insert(conversation);

                            boolean isSelf = false;
                            boolean isSecured = false;
                            if(isValidKey) {
                                try {
                                    boolean[] res = processForEncryptionKey(context, conversation);
                                    isSelf = res[0];
                                    isSecured = res[1];
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            ThreadedConversations threadedConversations =
                                    insertThreads(context, conversation, isSecured, isSelf);

                            Intent broadcastIntent = new Intent(DATA_DELIVER_ACTION);
                            broadcastIntent.putExtra(Conversation.ID, messageId);
                            broadcastIntent.putExtra(Conversation.THREAD_ID, threadId);
                            context.sendBroadcast(broadcastIntent);

                            if(!threadedConversations.isIs_mute())
                                NotificationsHandler.sendIncomingTextMessageNotification(context,
                                        conversation);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param context
     * @param conversation
     * @return true if isSelf and false otherwise
     * @throws Exception
     */
    boolean[] processForEncryptionKey(Context context, Conversation conversation) throws
            Exception {
        byte[] data = Base64.decode(conversation.getData(), Base64.DEFAULT);
        final String keystoreAlias = E2EEHandler.deriveKeystoreAlias(conversation.getAddress(), 0);
        byte[] extractedTransmissionKey = E2EEHandler.extractTransmissionKey(data);

        E2EEHandler.insertNewAgreementKeyDefault(context, extractedTransmissionKey, keystoreAlias);
        final boolean isSelf = E2EEHandler.isSelf(context, keystoreAlias);
//        Log.d(getClass().getName(), "Is self: " + isSelf);
//        Log.d(getClass().getName(), "Is secured: " + E2EEHandler
//                .canCommunicateSecurely(context, isSelf ?
//                        E2EEHandler.buildForSelf(keystoreAlias) : keystoreAlias, true));
        return new boolean[]{isSelf,
                E2EEHandler.canCommunicateSecurely(context, isSelf ?
                        E2EEHandler.buildForSelf(keystoreAlias) : keystoreAlias, true)};
    }
}
