package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import java.util.ArrayList;
import java.util.List;

import kotlin.coroutines.Continuation;


public class ConversationPagingSource extends PagingSource<Integer, Conversation> {

    String threadId;
    ConversationDao conversationDao;

    Integer initialkey;

    public ConversationPagingSource(ConversationDao conversationDao, String threadId,
                                    Integer initialKey) {
        this.conversationDao = conversationDao;
        this.threadId = threadId;
        this.initialkey = initialKey;
        Log.d(getClass().getName(), "Initialized with key: " + initialKey);
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Conversation> state) {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
//        Integer anchorPosition = state.getAnchorPosition();
        Integer anchorPosition = initialkey;
        Log.d(getClass().getName(), "Loading with key: " + initialkey);
//        return initialkey;
        if (anchorPosition == null) {
            return null;
        }

        LoadResult.Page<Integer, Conversation> anchorPage = state.closestPageToPosition(anchorPosition);
        if (anchorPage == null) {
            return null;
        }

        Integer prevKey = anchorPage.getPrevKey();
        if (prevKey != null) {
            return prevKey + 1;
        }

        Integer nextKey = anchorPage.getNextKey();
        if (nextKey != null) {
            return nextKey - 1;
        }
        return null;

    }

    @Nullable
    @Override
    public LoadResult<Integer, Conversation> load(
            @NonNull LoadParams<Integer> loadParams,
            @NonNull Continuation<? super LoadResult<Integer, Conversation>> continuation) {
        final List<Conversation>[] list = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                list[0] = conversationDao.getAll(threadId);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new LoadResult.Page<>(list[0],
                null,
                null,
//                loadParams.getKey() != null ? loadParams.getKey() + 1 : null,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }
}
