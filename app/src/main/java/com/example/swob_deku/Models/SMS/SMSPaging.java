package com.example.swob_deku.Models.SMS;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        return null;
    }

    @Nullable
    @Override
    public LoadResult load(@NonNull LoadParams<Integer> loadParams,
                           @NonNull Continuation<? super LoadResult<Integer, ArrayList<SMS>>> continuation) {
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
            return new LoadResult.Error<>(e);
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
