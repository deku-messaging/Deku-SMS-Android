package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.NativeSMSDB;

import java.util.ArrayList;
import java.util.List;

public class ConversationWorkManager extends Worker {
    public ConversationWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            loadConversationsFromNative(getApplicationContext());

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sharedPreferences.edit()
                    .putBoolean(CustomAppCompactActivity.LOAD_NATIVES, false)
                    .apply();
        } catch(Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }

    public void loadConversationsFromNative(Context context) {
        Cursor cursor = NativeSMSDB.fetchAll(context);
        List<Conversation> conversationList = new ArrayList<>();
        if(cursor.moveToNext()) {
            do {
                conversationList.add(Conversation.build(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        ConversationDao conversationDao = Conversation.getDao(context);
        conversationDao.insertAll(conversationList);
    }
}
