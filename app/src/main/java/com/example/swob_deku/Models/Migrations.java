package com.example.swob_deku.Models;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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

}
