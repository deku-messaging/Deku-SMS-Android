package com.afkanerd.deku.DefaultSMS.Models.Database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryptionDao;
import com.afkanerd.deku.E2EE.Security.CustomKeyStore;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientDAO;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjectDao;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjects;
import com.afkanerd.deku.Router.GatewayServers.GatewayServer;
import com.afkanerd.deku.Router.GatewayServers.GatewayServerDAO;
//import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient;
//import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientDAO;
//import com.afkanerd.deku.Router.GatewayServers.GatewayServer;
//import com.afkanerd.deku.Router.GatewayServers.GatewayServerDAO;

//@Database(entities = {GatewayServer.class, Archive.class, GatewayClient.class,
//        ThreadedConversations.class, Conversation.class}, version = 9)

@Database(entities = {
        ThreadedConversations.class,
        CustomKeyStore.class,
        Archive.class,
        GatewayServer.class,
        GatewayClientProjects.class,
        ConversationsThreadsEncryption.class,
        Conversation.class,
        GatewayClient.class},
        version = 11, autoMigrations = {@AutoMigration(from = 10, to = 11)})
public abstract class Datastore extends RoomDatabase {
    public static Datastore datastore;

    public static String databaseName = "SMSWithoutBorders-Messaging-DB";

    public abstract GatewayServerDAO gatewayServerDAO();

    public abstract GatewayClientDAO gatewayClientDAO();
    public abstract GatewayClientProjectDao gatewayClientProjectDao();

    public abstract ThreadedConversationsDao threadedConversationsDao();

    public abstract ConversationDao conversationDao();

    public abstract CustomKeyStoreDao customKeyStoreDao();

    public abstract ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao();


    @Override
    public void clearAllTables() {

    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull DatabaseConfiguration databaseConfiguration) {
        return null;
    }
}
