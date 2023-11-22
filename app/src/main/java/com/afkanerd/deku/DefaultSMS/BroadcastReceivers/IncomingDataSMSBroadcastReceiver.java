package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Log;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.google.i18n.phonenumbers.NumberParseException;

//import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class IncomingDataSMSBroadcastReceiver extends BroadcastReceiver {

    public static String DATA_DELIVER_ACTION = BuildConfig.APPLICATION_ID + ".DATA_DELIVER_ACTION" ;

    public static String DATA_SENT_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_SENT_BROADCAST_INTENT";

    public static String DATA_DELIVERED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_DELIVERED_BROADCAST_INTENT";

    public static String DATA_UPDATED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".DATA_UPDATED_BROADCAST_INTENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * Important note: either image or dump it
         */

        Log.d(getClass().getName(), "Broadcast data received: " + intent.getAction());

        if (intent.getAction().equals(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION)) {
            Log.d(getClass().getName(), "Yes new data received");
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

                    boolean isValidKey = E2EEHandler.isValidDekuPublicKey( Base64.decode(data, Base64.DEFAULT));

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

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(isValidKey) {
                                try {
                                    processForEncryptionKey(context, conversation);
                                } catch (NumberParseException | CertificateException |
                                         KeyStoreException | IOException |
                                         NoSuchAlgorithmException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            ConversationDao conversationDao = Conversation.getDao(context);
                            conversationDao.insert(conversation);

                            NotificationsHandler.sendIncomingTextMessageNotification(context,
                                    conversation);

                            Intent broadcastIntent = new Intent(DATA_DELIVER_ACTION);
                            broadcastIntent.putExtra(Conversation.ID, messageId);
                            broadcastIntent.putExtra(Conversation.THREAD_ID, threadId);
                            context.sendBroadcast(broadcastIntent);

                        }
                    }).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean processForEncryptionKey(Context context, Conversation conversation) throws NumberParseException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, InterruptedException {
        byte[] data = Base64.decode(conversation.getData(), Base64.DEFAULT);
        boolean isValidKey = E2EEHandler.isValidDekuPublicKey(data);

        if(isValidKey) {
            String keystoreAlias = E2EEHandler.getKeyStoreAlias(conversation.getAddress(), 0);
            byte[] extractedTransmissionKey = E2EEHandler.extractTransmissionKey(data);
            // TODO
            switch(E2EEHandler.getKeyType(context, keystoreAlias, extractedTransmissionKey)) {
                case E2EEHandler.REQUEST_KEY:
                    break;
                case E2EEHandler.AGREEMENT_KEY:
                    break;
                case E2EEHandler.IGNORE_KEY:
                default:
                    break;
            }
            E2EEHandler.insertNewPeerPublicKey(context, extractedTransmissionKey, keystoreAlias);
        }
        Log.d(getClass().getName(), "Is Encrypted data: " + isValidKey);

        return isValidKey;
    }
}
