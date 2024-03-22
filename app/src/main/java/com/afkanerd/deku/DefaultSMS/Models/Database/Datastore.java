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
        version = 14,
        autoMigrations = {
        @AutoMigration(from = 11, to = 12),
        @AutoMigration(from = 12, to = 13),
        @AutoMigration(from = 13, to = 14)
})
public abstract class Datastore extends RoomDatabase {
    private static Datastore datastore;

    public static Datastore getDatastore(Context context) {
        if(datastore == null || !datastore.isOpen()) {
            datastore = Room.databaseBuilder(context, Datastore.class, databaseName)
                    .enableMultiInstanceInvalidation()
                    .addMigrations(new Migrations.Migration4To5())
                    .addMigrations(new Migrations.Migration5To6())
                    .addMigrations(new Migrations.Migration6To7())
                    .addMigrations(new Migrations.Migration7To8())
                    .addMigrations(new Migrations.Migration9To10())
                    .addMigrations(new Migrations.Migration10To11(context))
                    .addMigrations(new Migrations.MIGRATION_11_12())
                    .build();
        }

        return datastore;
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
