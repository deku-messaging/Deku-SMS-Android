package com.afkanerd.deku.DefaultSMS.Models.Database;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientListingActivity;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjects;

public class Migrations {
    // Define the migration class
    public static class Migration4To5 extends Migration {
        public Migration4To5() {
            super(4, 5);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Step 1: Create the new table
//            database.execSQL("CREATE TABLE IF NOT EXISTS new_table (id INTEGER PRIMARY KEY, name TEXT)");
        }
    }

    public static class Migration5To6 extends Migration {
        public Migration5To6() {
            super(5, 6);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Step 1: Create the new table
            database.execSQL("ALTER TABLE GatewayClient ADD COLUMN projectBinding2 TEXT");
        }
    }

    public static class Migration6To7 extends Migration {
        public Migration6To7() {
            super(6, 7);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS GatewayServer");

            database.execSQL("CREATE TABLE IF NOT EXISTS GatewayServer (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "format TEXT, date INTEGER, " +
                    "protocol TEXT, URL TEXT)");

        }
    }

    public static class Migration7To8 extends Migration {
        public Migration7To8() {
            super(7, 8);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE GatewayServer ADD COLUMN tag TEXT");
        }
    }

    public static class Migration8To9 extends Migration {
        public Migration8To9() {
            super(8, 9);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS ThreadedConversations " +
                    "(thread_id TEXT PRIMARY KEY NOT NULL, " +
                    "msg_count INTEGER NOT NULL, " +
                    "type INTEGER NOT NULL, " +
                    "date TEXT, " +
                    "is_archived INTEGER NOT NULL, " +
                    "is_blocked INTEGER NOT NULL, " +
                    "is_read INTEGER NOT NULL, " +
                    "is_shortcode INTEGER NOT NULL, " +
                    "snippet TEXT, " +
                    "contact_name TEXT, " +
                    "address TEXT, " +
                    "formatted_datetime TEXT)");

            database.execSQL("CREATE TABLE IF NOT EXISTS Conversation " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "message_id TEXT, " +
                    "thread_id TEXT, " +
                    "date TEXT, " +
                    "date_sent TEXT, " +
                    "type INTEGER NOT NULL, " +
                    "num_segments INTEGER NOT NULL, " +
                    "subscription_id INTEGER NOT NULL, " +
                    "error_code INTEGER NOT NULL, " +
                    "status INTEGER NOT NULL, " +
                    "read INTEGER NOT NULL, " +
                    "is_encrypted INTEGER NOT NULL, " +
                    "is_key INTEGER NOT NULL, " +
                    "is_image INTEGER NOT NULL, " +
                    "formatted_date TEXT, " +
                    "address TEXT, " +
                    "text TEXT, " +
                    "data TEXT)");

            database.execSQL("CREATE TABLE IF NOT EXISTS ConversationsThreadsEncryption " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "publicKey TEXT, " +
                    "keystoreAlias TEXT, " +
                    "exchangeDate INTEGER NOT NULL)");

            database.execSQL("UPDATE ThreadedConversations SET is_archived = 1 " +
                    "WHERE thread_id IN (SELECT threadId as thread_id FROM Archive)");

            database.execSQL("DROP TABLE IF EXISTS Archive");
            database.execSQL("CREATE TABLE IF NOT EXISTS Archive " +
                    "(thread_id TEXT PRIMARY KEY NOT NULL, is_archived INTEGER NOT NULL)");

            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_Conversation_message_id ON Conversation (message_id)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ConversationsThreadsEncryption_keystoreAlias " +
                    "ON ConversationsThreadsEncryption (keystoreAlias)");
        }
    }

    public static class Migration9To10 extends Migration {
        public Migration9To10() {
            super(9, 10);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ConversationsThreadsEncryption ADD COLUMN states TEXT");
            database.execSQL("ALTER TABLE ConversationsThreadsEncryption ADD COLUMN _mk TEXT");
        }
    }

    public static class Migration10To11 extends Migration {
        public Migration10To11(Context context) {
            super(10, 11);
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(GatewayClientHandler.MIGRATIONS, Context.MODE_PRIVATE);
            if(!sharedPreferences.contains(GatewayClientHandler.MIGRATIONS_TO_11)) {
                context.getSharedPreferences(GatewayClientHandler.MIGRATIONS, Context.MODE_PRIVATE)
                        .edit().putBoolean(GatewayClientHandler.MIGRATIONS_TO_11, true).apply();
                sharedPreferences =
                        context.getSharedPreferences(GatewayClientListingActivity.Companion
                                .getGATEWAY_CLIENT_LISTENERS(), Context.MODE_PRIVATE);
                for(String key : sharedPreferences.getAll().keySet()) {
                    sharedPreferences.edit().remove(key).apply();
                }
            }
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
        }
    }

    public static class MIGRATION_11_12 extends Migration {

        public MIGRATION_11_12() {
            super(11, 12);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            Log.d(getClass().getName(), "Migration to 12 happening");
            supportSQLiteDatabase.execSQL("ALTER TABLE ThreadedConversations " +
                    "ADD COLUMN is_mute INTEGER NOT NULL DEFAULT 0");
            supportSQLiteDatabase.execSQL("ALTER TABLE ThreadedConversations " +
                    "ADD COLUMN is_secured INTEGER NOT NULL DEFAULT 0");
        }
    };

}
