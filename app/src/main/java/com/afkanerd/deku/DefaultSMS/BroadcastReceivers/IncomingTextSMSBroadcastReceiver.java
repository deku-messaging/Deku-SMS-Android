package com.afkanerd.deku.DefaultSMS.BroadcastReceivers;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver.DATA_DELIVERED_BROADCAST_INTENT;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver.DATA_SENT_BROADCAST_INTENT;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingDataSMSBroadcastReceiver.DATA_UPDATED_BROADCAST_INTENT;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationHandler;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.SemaphoreManager;
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB;
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler;
import com.afkanerd.deku.E2EE.E2EEHandler;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerHandler;
import com.afkanerd.deku.Router.Router.RouterItem;
import com.afkanerd.deku.Router.Router.RouterHandler;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncomingTextSMSBroadcastReceiver extends BroadcastReceiver {
    Context context;

    public static final String TAG_NAME = "RECEIVED_SMS_ROUTING";
    public static final String TAG_ROUTING_URL = "swob.work.route.url,";


    public static String SMS_DELIVER_ACTION =
            BuildConfig.APPLICATION_ID + ".SMS_DELIVER_ACTION";
    public static String SMS_SENT_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_SENT_BROADCAST_INTENT";
    public static String SMS_UPDATED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_UPDATED_BROADCAST_INTENT";

    public static String SMS_DELIVERED_BROADCAST_INTENT =
            BuildConfig.APPLICATION_ID + ".SMS_DELIVERED_BROADCAST_INTENT";


    /*
    - address received might be different from how address is saved.
    - how it received is the trusted one, but won't match that which has been saved.
    - when message gets stored it's associated to the thread - so matching is done by android
    - without country code, can't know where message is coming from. Therefore best assumption is
    - service providers do send in country code.
    - How is matched to users stored without country code?
     */

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_DELIVER_ACTION)) {
            if (getResultCode() == Activity.RESULT_OK) {
                try {
                    final String[] regIncomingOutput = NativeSMSDB.Incoming.register_incoming_text(context, intent);
                    if(regIncomingOutput != null) {
                        final String messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID];
                        final String incomingText = regIncomingOutput[NativeSMSDB.BODY];
                        final String threadId = regIncomingOutput[NativeSMSDB.THREAD_ID];
                        final String address = regIncomingOutput[NativeSMSDB.ADDRESS];
                        final String date = regIncomingOutput[NativeSMSDB.DATE];
                        final String dateSent = regIncomingOutput[NativeSMSDB.DATE_SENT];
                        final int subscriptionId = Integer.parseInt(regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID]);

                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                String text = incomingText;
                                try {
                                    text = processEncryptedIncoming(context, address, incomingText);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                                Conversation conversation = new Conversation();
                                conversation.setMessage_id(messageId);
                                conversation.setText(text);
                                conversation.setThread_id(threadId);
                                conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
                                conversation.setAddress(address);
                                conversation.setSubscription_id(subscriptionId);
                                conversation.setDate(date);
                                conversation.setDate_sent(dateSent);

                                try {
                                    conversation.getDaoInstance(context).insert(conversation);
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }

                                Intent broadcastIntent = new Intent(SMS_DELIVER_ACTION);
                                broadcastIntent.putExtra(Conversation.ID, messageId);
                                context.sendBroadcast(broadcastIntent);


                                String defaultRegion = Helpers.getUserCountry(context);
                                String e16Address = Helpers.getFormatCompleteNumber(address, defaultRegion);
                                if(!Contacts.isMuted(context, e16Address) &&
                                        !Contacts.isMuted(context, address))
                                    NotificationsHandler.sendIncomingTextMessageNotification(context,
                                            conversation);
                                router_activities(messageId);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else if(intent.getAction().equals(SMS_SENT_BROADCAST_INTENT)) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String id = intent.getStringExtra(NativeSMSDB.ID);

                    Conversation conversation = ConversationHandler.acquireDatabase(context)
                            .conversationDao().getMessage(id);
                    if(conversation == null)
                        return;

                    if (getResultCode() == Activity.RESULT_OK) {
                        NativeSMSDB.Outgoing.register_sent(context, id);
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_NONE);
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
                    } else {
                        try {
                            NativeSMSDB.Outgoing.register_failed(context, id, getResultCode());
                            conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                            conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                            conversation.setError_code(getResultCode());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    ConversationHandler.acquireDatabase(context)
                            .conversationDao().update(conversation);

                    Intent broadcastIntent = new Intent(SMS_UPDATED_BROADCAST_INTENT);
                    broadcastIntent.putExtra(Conversation.ID, conversation.getMessage_id());
                    broadcastIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());

                    if(intent.getExtras() != null)
                        broadcastIntent.putExtras(intent.getExtras());

                    context.sendBroadcast(broadcastIntent);
                }
            });
        }
        else if(intent.getAction().equals(SMS_DELIVERED_BROADCAST_INTENT)) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String id = intent.getStringExtra(NativeSMSDB.ID);

                    Conversation conversation = ConversationHandler.acquireDatabase(context)
                            .conversationDao().getMessage(id);
                    if(conversation == null)
                        return;

                    if (getResultCode() == Activity.RESULT_OK) {
                        NativeSMSDB.Outgoing.register_delivered(context, id);
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_COMPLETE);
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
                    } else {
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                        conversation.setError_code(getResultCode());
                    }

                    ConversationHandler.acquireDatabase(context)
                            .conversationDao().update(conversation);

                    Intent broadcastIntent = new Intent(SMS_UPDATED_BROADCAST_INTENT);
                    broadcastIntent.putExtra(Conversation.ID, conversation.getMessage_id());
                    broadcastIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());
                    if(intent.getExtras() != null)
                        broadcastIntent.putExtras(intent.getExtras());

                    context.sendBroadcast(broadcastIntent);
                }
            });
        }


        else if(intent.getAction().equals(DATA_SENT_BROADCAST_INTENT)) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String id = intent.getStringExtra(NativeSMSDB.ID);
                    Conversation conversation1 = new Conversation();
                    ConversationDao conversationDao = conversation1.getDaoInstance(context);
                    Conversation conversation = conversationDao.getMessage(id);

                    if (getResultCode() == Activity.RESULT_OK) {
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_NONE);
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
                    } else {
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                        conversation.setError_code(getResultCode());
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                    }
                    conversationDao.update(conversation);

                    Intent broadcastIntent = new Intent(DATA_UPDATED_BROADCAST_INTENT);
                    broadcastIntent.putExtra(Conversation.ID, conversation.getMessage_id());
                    broadcastIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());

                    context.sendBroadcast(broadcastIntent);
                }
            });
        }
        else if(intent.getAction().equals(DATA_DELIVERED_BROADCAST_INTENT)) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String id = intent.getStringExtra(NativeSMSDB.ID);
                    Conversation conversation1 = new Conversation();
                    ConversationDao conversationDao = conversation1.getDaoInstance(context);
                    Conversation conversation = conversationDao.getMessage(id);

                    if (getResultCode() == Activity.RESULT_OK) {
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_COMPLETE);
                        conversation.setType(Telephony.TextBasedSmsColumns.STATUS_COMPLETE);
                    } else {
                        conversation.setStatus(Telephony.TextBasedSmsColumns.STATUS_FAILED);
                        conversation.setError_code(getResultCode());
                        conversation.setType(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);
                    }

                    conversationDao.update(conversation);

                    Intent broadcastIntent = new Intent(DATA_UPDATED_BROADCAST_INTENT);
                    broadcastIntent.putExtra(Conversation.ID, conversation.getMessage_id());
                    broadcastIntent.putExtra(Conversation.THREAD_ID, conversation.getThread_id());

                    context.sendBroadcast(broadcastIntent);
                }
            });
        }
    }

    public String processEncryptedIncoming(Context context, String address, String text) throws Throwable {
        if(E2EEHandler.isValidDefaultText(text)) {
            String keystoreAlias = E2EEHandler.deriveKeystoreAlias(address, 0);
            byte[] cipherText = E2EEHandler.extractTransmissionText(text);
            text = new String(E2EEHandler.decrypt(context, keystoreAlias, cipherText, null, null));
        }
        return text;
    }

    public void router_activities(String messageId) {
        try {
            Cursor cursor = NativeSMSDB.fetchByMessageId(context, messageId);
            if(cursor.moveToFirst()) {
                GatewayServerHandler gatewayServerHandler = new GatewayServerHandler(context);
                RouterItem routerItem = new RouterItem(cursor);
                RouterHandler.route(context, routerItem, gatewayServerHandler);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}