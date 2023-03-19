package com.example.swob_deku.Models.SMS;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.ListenableFuturePagingSource;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;
import androidx.paging.rxjava2.RxPagingSource;

import com.example.swob_deku.BuildConfig;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import kotlin.coroutines.Continuation;

public class SMSPaging extends PagingSource<Integer, SMS> {
    Context context;

    String threadId;

    public Integer lastUsedKey;

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
        lastUsedKey = key;

        int smsOffset = key == 0 ? key : (loadParams.getLoadSize() * key) - 3;

        ArrayList<SMS> smsArrayList = fetchSMSFromHandlers(context, threadId, loadParams.getLoadSize(), smsOffset);

        if(smsArrayList == null || smsArrayList.isEmpty()) {
//            return new LoadResult.Page<>(smsArrayList,
//                    null,
//                    null,
//                    LoadResult.Page.COUNT_UNDEFINED,
//                    LoadResult.Page.COUNT_UNDEFINED);
//            return new LoadResult.Invalid<>();
        }

        try {
            return new LoadResult.Page<>(smsArrayList,
                    key == 0 ? null : key - 1,
//                    smsArrayList.size() < loadParams.getLoadSize() ? null : key + 1,
                    key + 1,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        } catch(Exception e) {
            return new LoadResult.Error<>(e);
        }
    }

    public static ArrayList<SMS> fetchSMSFromHandlers(Context context, String threadId, int limit, int offset) {
//        Cursor cursors = SMSHandler.fetchSMSForThread(this.context, this.threadId);
        Cursor cursors = SMSHandler.fetchSMSForThread(context, threadId, limit, offset);

        ArrayList<SMS> smsArrayList = new ArrayList<>();

        if(cursors == null)
            return null;

        if(BuildConfig.DEBUG)
            Log.d(SMSPaging.class.getName(), "Paging fetched: " + cursors.getCount());

        if(cursors.moveToFirst()) {
            do {
                smsArrayList.add(new SMS(cursors));
            } while(cursors.moveToNext());
        }
        cursors.close();

        return smsArrayList;
    }
}
