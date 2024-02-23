package java.com.afkanerd.deku;

import android.content.Context;
import android.database.sqlite.SQLiteStatement;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)

public class RoomMigrationTest {
    private static final String TEST_DB = Datastore.databaseName;

    @Rule
    public MigrationTestHelper helper;

    Context context;
    public RoomMigrationTest() {
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                Datastore.class.getCanonicalName(), new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate11To12Test() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 11);
        String tableName = "ThreadedConversations";

        String sql = "INSERT INTO " + tableName + " ("
                + "thread_id, "
                + "address, "
                + "msg_count, "
                + "type, "
                + "date, "
                + "is_archived, "
                + "is_blocked, "
                + "is_shortcode, "
                + "is_read, "
                + "snippet, "
                + "contact_name, "
                + "formatted_datetime, "
                + "is_read"
                + ") VALUES ";

        // Add each row as a separate VALUES clause
        sql += "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

        // Prepare the SQL statement with placeholders
        SupportSQLiteStatement statement = db.compileStatement(sql);

        // Bind values for each row
        statement.bindString(1, "test_thread_id_1");
        statement.bindString(2, "test_address_1");
        statement.bindLong(3, 5);
        statement.bindLong(13, 5);
        statement.bindString(4, "test_address_1");
        statement.bindLong(5, 5);
        statement.bindLong(6, 5);
        statement.bindLong(7, 5);
        statement.bindLong(8, 5);
        statement.bindString(9, "test_address_1");
        statement.bindString(10, "test_address_1");
        statement.bindString(11, "test_address_1");
        statement.bindLong(12, 5);

        // Execute the statement
        statement.execute();

        // Close the statement
        statement.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 12, true,
                new Migrations.MIGRATION_11_12());
    }

}
