package com.afkanerd.deku;

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
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;
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
        version = 15,
        autoMigrations = {
        @AutoMigration(from = 9, to = 10),
        @AutoMigration(from = 10, to = 11),
        @AutoMigration(from = 11, to = 12),
        @AutoMigration(from = 12, to = 13),
        @AutoMigration(from = 13, to = 14),
        @AutoMigration(from = 14, to = 15)
})
public abstract class Datastore extends RoomDatabase {
    private static volatile Datastore datastore;

    public static synchronized Datastore getDatastore(Context context) {
        if(datastore == null) {
            datastore = create(context);
        }
        return datastore;
    }

    private static Datastore create(final Context context) {
        return Room.databaseBuilder(context, Datastore.class, databaseName)
                .enableMultiInstanceInvalidation()
                .build();
    }

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
