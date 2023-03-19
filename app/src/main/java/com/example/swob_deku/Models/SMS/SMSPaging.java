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

    public ArrayList<SMS> fetchedMessages;

    public SMSPaging(Context context, String threadId, ArrayList<SMS> fetchedMessages) {
        Log.d(getClass().getName(), "Paging constructor called!");
        this.context = context;
        this.threadId = threadId;

        this.fetchedMessages = fetchedMessages == null ? new ArrayList<>() : fetchedMessages;
    }

    public SMSPaging(Context context, String threadId) {
        Log.d(getClass().getName(), "Paging constructor called!");
        this.context = context;
        this.threadId = threadId;

        this.fetchedMessages = fetchedMessages == null ? new ArrayList<>() : fetchedMessages;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, SMS> pagingState) {
//
//        Integer anchorPosition = pagingState.getAnchorPosition();
//
//        if(anchorPosition == null) {
//            return null;
//        }
//
//        LoadResult.Page<Integer, SMS> anchorPage = pagingState.closestPageToPosition(anchorPosition);
//        if(anchorPage == null)
//            return null;
//
//        Integer prevKey = anchorPage.getPrevKey();
//        Log.d(getClass().getName(), "Paging refresh previous key: " + prevKey);
//        if(prevKey != null)
//            return prevKey;
//
//        Integer nextKey = anchorPage.getNextKey();
//        Log.d(getClass().getName(), "Paging refresh next key: " + nextKey);
//        if(nextKey != null)
//            return nextKey;
//
        return null;
    }

    @Nullable
    @Override
    public LoadResult load(@NonNull LoadParams<Integer> loadParams,
                           @NonNull Continuation<? super LoadResult<Integer, SMS>> continuation) {

        int offset = loadParams.getKey() == null ? 0 : loadParams.getKey();
        Log.d(getClass().getName(), "Paging offset: " + offset);

        ArrayList<SMS> smsArrayList = fetchSMSFromHandlers(loadParams.getLoadSize(), offset);

        if(smsArrayList == null || smsArrayList.isEmpty()) {
            return new LoadResult.Page<>(fetchedMessages,
                    null,
                    null,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        }
        else {
            if(offset != 0 && !fetchedMessages.isEmpty())
                fetchedMessages.addAll(smsArrayList);
            else
                fetchedMessages = smsArrayList;
            Log.d(getClass().getName(), "Paging fetched total size: " + fetchedMessages.size());
        }

        try {
            return new LoadResult.Page<>(fetchedMessages,
                    null,
                    smsArrayList.size() >= loadParams.getLoadSize()?
                            offset + loadParams.getLoadSize() : null,
                    LoadResult.Page.COUNT_UNDEFINED,
                    LoadResult.Page.COUNT_UNDEFINED);
        } catch(Exception e) {
            return new LoadResult.Error<>(e);
        }
    }



    private ArrayList<SMS> fetchSMSFromHandlers(int limit, int offset) {
//        Cursor cursors = SMSHandler.fetchSMSForThread(this.context, this.threadId);
        if(BuildConfig.DEBUG) {
            Log.d(getClass().getName(), "Paging fetched load: " + limit);
            Log.d(getClass().getName(), "Paging fetched offset: " + offset);
        }
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
