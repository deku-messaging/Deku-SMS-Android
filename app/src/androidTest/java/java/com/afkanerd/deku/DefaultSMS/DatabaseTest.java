package java.com.afkanerd.deku.DefaultSMS;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(AndroidJUnit4.class)
public class DatabaseTest {

    Context context;

    public DatabaseTest() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    @Test
    public void testThreads() {
        Cursor cursor = context.getContentResolver().query(
                Telephony.Threads.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        String[] columnNames = cursor.getColumnNames();
        Log.d(getClass().getName(), Telephony.Threads.CONTENT_URI.toString());

        if(cursor.moveToFirst()) {
            do {
                for(String columnName : columnNames)
                    Log.d(getClass().getName(), columnName + ": " +
                            cursor.getString(cursor.getColumnIndex(columnName)));
                Log.d(getClass().getName(), "\n");
            } while(cursor.moveToNext());
        }
    }
}
