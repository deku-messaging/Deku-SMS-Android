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

    public SMSPaging(Context context, String threadId) {
        Log.d(getClass().getName(), "Paging constructor called!");
        this.context = context;
        this.threadId = threadId;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, SMS> pagingState) {
        Log.d(getClass().getName(), "Paging refreshkey called!");

        Integer anchorPosition = pagingState.getAnchorPosition();

        if(anchorPosition == null) {
            return null;
        }

        LoadResult.Page<Integer, SMS> anchorPage = pagingState.closestPageToPosition(anchorPosition);
        if(anchorPage == null)
            return null;

        Integer prevKey = anchorPage.getPrevKey();
        if(prevKey != null)
            return prevKey + 1;

        Integer nextKey = anchorPage.getNextKey();
        if(nextKey != null)
            return nextKey -1;

        return null;
    }

    @Nullable
    @Override
    public LoadResult load(@NonNull LoadParams<Integer> loadParams,
                           @NonNull Continuation<? super LoadResult<Integer, SMS>> continuation) {
//        Log.d(getClass().getName(), "Paging load called!");

        int offset = loadParams.getKey() == null ? 0 : loadParams.getKey();

        ArrayList<SMS> smsArrayList = fetchSMSFromHandlers(loadParams.getLoadSize(), offset);
        offset += loadParams.getLoadSize();

        if(smsArrayList == null || smsArrayList.isEmpty())
            return new LoadResult.Invalid();

        try {
            return new LoadResult.Page<>(smsArrayList,
                    null,
                    offset,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        } catch(Exception e) {
            return new LoadResult.Error<>(e);
        }
    }

    private ArrayList<SMS> fetchSMSFromHandlers(int limit, int offset) {
//        Cursor cursors = SMSHandler.fetchSMSForThread(this.context, this.threadId);
        Cursor cursors = SMSHandler.fetchSMSForThread(this.context, this.threadId, limit, offset);

        ArrayList<SMS> smsArrayList = new ArrayList<>();

        if(cursors == null)
            return null;

        if(BuildConfig.DEBUG)
            Log.d(getClass().getName(), "Paging fetched: " + cursors.getCount());

        if(cursors.moveToFirst()) {
            do {
                smsArrayList.add(new SMS(cursors));
            } while(cursors.moveToNext());
        }
        cursors.close();

        return smsArrayList;
    }
}
