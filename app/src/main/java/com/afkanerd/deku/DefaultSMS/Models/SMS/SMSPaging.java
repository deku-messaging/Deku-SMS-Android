package com.afkanerd.deku.DefaultSMS.Models.SMS;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.afkanerd.deku.DefaultSMS.BuildConfig;

import java.util.ArrayList;

import kotlin.coroutines.Continuation;

public class SMSPaging extends PagingSource<Integer, SMS> {
    Context context;

    String threadId;

    public Integer lastUsedKey, nextKey;

    public SMSPaging(Context context, String threadId, ArrayList<SMS> fetchedMessages) {
        Log.d(getClass().getName(), "Paging constructor called!");
        this.context = context;
        this.threadId = threadId;
    }

    public SMSPaging(Context context, String threadId) {
        Log.d(getClass().getName(), "Paging constructor called!");
        this.context = context;
        this.threadId = threadId;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, SMS> pagingState) {

        Integer anchorPosition = pagingState.getAnchorPosition();
        Log.d(getClass().getName(), "Paging anchor position: " + anchorPosition);
        if(anchorPosition == null) {
            return null;
        }

        LoadResult.Page<Integer, SMS> anchorPage = pagingState.closestPageToPosition(anchorPosition);
        if(anchorPage == null)
            return null;

//        Integer prevKey = anchorPage.getPrevKey();
//        Log.d(getClass().getName(), "Paging anchor position prevKey: " + prevKey);
//        if(prevKey != null)
//            return prevKey;

        Integer nextKey = anchorPage.getNextKey();
        Log.d(getClass().getName(), "Paging anchor position nextKey: " + nextKey);
        if(nextKey != null)
            return nextKey;

        return null;
    }

    @Nullable
    @Override
    public LoadResult load(@NonNull LoadParams<Integer> loadParams,
                           @NonNull Continuation<? super LoadResult<Integer, SMS>> continuation) {

        int key = loadParams.getKey() == null ? 0 : loadParams.getKey();
        Log.d(getClass().getName(), "Paging Load key: " + key);

        int smsLimit = loadParams.getKey() == null ? loadParams.getLoadSize() : (loadParams.getLoadSize() * (key + 1));

        ArrayList<SMS> smsArrayList = fetchMessages_advanced(context, threadId, smsLimit, 0);

        nextKey = (smsArrayList == null || smsArrayList.size() < loadParams.getLoadSize() || smsArrayList.size() < smsLimit) ?
                null : key + 1;
        Log.d(getClass().getName(), "Paging next key: " + nextKey);

        lastUsedKey = key;
        if(smsArrayList == null || smsArrayList.isEmpty() || nextKey == null) {
            Log.d(getClass().getName(), "Paging should have hit limit: " + smsArrayList.size() + ":" + smsLimit);
            return new LoadResult.Page<>(smsArrayList,
                    null,
                    null,
                    LoadResult.Page.COUNT_UNDEFINED,
                    0);
        }
        try {
            return new LoadResult.Page<>(smsArrayList,
                    null,
                    nextKey,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        } catch(Exception e) {
            return new LoadResult.Error<>(e);
        }
    }

    public static ArrayList<SMS> fetchMessages_advanced(Context context, String threadId, int limit, int offset) {
//        Cursor cursors = SMSHandler.fetchSMSForThread(this.context, this.threadId);
        SMSMetaEntity smsMetaEntity = new SMSMetaEntity();
        smsMetaEntity.setThreadId(context, threadId);
        Cursor cursors = smsMetaEntity.fetchMessages(context, limit, offset);

        ArrayList<SMS> smsArrayList = new ArrayList<>();

        if(cursors == null)
            return null;

        if(BuildConfig.DEBUG)
            Log.d(SMSPaging.class.getName(), "Paging fetched: " + cursors.getCount() + ":" + limit);

        if(cursors.moveToFirst()) {
            do {
                smsArrayList.add(new SMS(cursors));
            } while(cursors.moveToNext());
        }
        cursors.close();

        return smsArrayList;
    }
}
