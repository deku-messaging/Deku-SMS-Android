package com.example.swob_deku.Models.SMS;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.ListenableFuturePagingSource;
import androidx.paging.PagingState;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

public class SMSPaging extends ListenableFuturePagingSource<Integer, ArrayList<SMS>> {
    Context context;

    String threadId;

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, ArrayList<SMS>> pagingState) {
        Log.d(getClass().getName(), "Paging refreshkey called!");
        return null;
    }


    @NonNull
    @Override
    public ListenableFuture<LoadResult<Integer, ArrayList<SMS>>> loadFuture(@NonNull LoadParams<Integer> loadParams) {
        Log.d(getClass().getName(), "Paging source called!");
        final int fetchSize = 2;
        Integer startPos = loadParams.getKey();
        if(startPos == null) {
            startPos = 0;
        }

        ArrayList<Cursor> cursors = new ArrayList<>();
        try {
            cursors = SMSHandler.fetchSMSForThreadWithPaging(this.context,
                    this.threadId, startPos, fetchSize);
        } catch(Exception e) {
//            return new LoadResult.Error<>(e);
        }

        ArrayList<SMS> smsArrayList = new ArrayList<>();
        return null;
//        if(cursors == null) {
//            return Futures.transform(new LoadResult.Page<>(smsArrayList,
//                    null,
//                    null,
//                    LoadResult.Page.COUNT_UNDEFINED,
//                    LoadResult.Page.COUNT_UNDEFINED);
//        }
//
//        for(Cursor cursor: cursors)
//            smsArrayList.add(new SMS(cursor));
//
//        for(Cursor cursor: cursors)
//            cursor.close();
//
//        Log.d(getClass().getName(), "Paging fetched: " + smsArrayList.size());
//
//        return new LoadResult.Page<>(smsArrayList,
//                null,
//                smsArrayList.size() < (startPos + fetchSize) ? null : startPos + 1,
//                LoadResult.Page.COUNT_UNDEFINED,
//                LoadResult.Page.COUNT_UNDEFINED);
    }
}
