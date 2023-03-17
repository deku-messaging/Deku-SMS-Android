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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

import io.reactivex.Single;
import kotlin.coroutines.Continuation;

public class SMSPaging extends PagingSource<Integer, ArrayList<SMS>> {
    Context context;

    String threadId;

    public SMSPaging(Context context, String threadId) {
        this.context = context;
        this.threadId = threadId;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, ArrayList<SMS>> pagingState) {
        Log.d(getClass().getName(), "Paging refreshkey called!");
        return null;
    }

    @Nullable
    @Override
    public LoadResult load(@NonNull LoadParams<Integer> loadParams, @NonNull Continuation<? super LoadResult<Integer, ArrayList<SMS>>> continuation) {
        Log.d(getClass().getName(), "Paging source called!");
        final int fetchSize = 2;
        Integer startPos = loadParams.getKey();
        if(startPos == null) {
            startPos = 0;
        }

        ArrayList<Cursor> cursors = new ArrayList<>();
        Log.d(getClass().getName(), "Paging fetched: " + cursors.size());

        try {
            cursors = SMSHandler.fetchSMSForThreadWithPaging(this.context,
                    this.threadId, startPos, fetchSize);
        } catch(Exception e) {
//            return new LoadResult.Error<>(e);
        }

        ArrayList<SMS> smsArrayList = new ArrayList<>();
        if(cursors == null) {
            return new LoadResult.Page<>(smsArrayList,
                    null,
                    null,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        }

        for(Cursor cursor: cursors)
            smsArrayList.add(new SMS(cursor));

        for(Cursor cursor: cursors)
            cursor.close();


        return new LoadResult.Page<>(smsArrayList,
                null,
                smsArrayList.size() < (startPos + fetchSize) ? null : startPos + 1,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }
}
