package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;

import java.util.ArrayList;
import java.util.List;

import kotlin.coroutines.Continuation;

public class ThreadsPagingSource extends PagingSource<Integer, ThreadedConversations> {

    Context context;
    public ThreadsPagingSource(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, ThreadedConversations> state) {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
        Integer anchorPosition = state.getAnchorPosition();
        if (anchorPosition == null) {
            return null;
        }

        LoadResult.Page<Integer, ThreadedConversations> anchorPage = state.closestPageToPosition(anchorPosition);
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
    public Object load(@NonNull LoadParams<Integer> loadParams, @NonNull Continuation<? super LoadResult<Integer, ThreadedConversations>> continuation) {
        List<ThreadedConversations> threadedConversationsList = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(
                Telephony.Threads.CONTENT_URI,
                null,
                null,
                null,
                "date DESC"
        );
        if(cursor != null && cursor.moveToFirst()) {
            do {
                ThreadedConversations threadedConversations = new ThreadedConversations();
                int recipientIdIndex = cursor.getColumnIndex("address");
                int snippetIndex = cursor.getColumnIndex("body");
                int dateIndex = cursor.getColumnIndex("date");
                int threadIdIndex = cursor.getColumnIndex("thread_id");
                int typeIndex = cursor.getColumnIndex("type");
//                int isArchivedIndex = cursor.getColumnIndex(Telephony.Threads.ARCHIVED);

                threadedConversations.setAddress(cursor.getString(recipientIdIndex));
                if(threadedConversations.getAddress() == null || threadedConversations.getAddress().isEmpty())
                    continue;
                String contactName = Contacts.retrieveContactName(context,
                        threadedConversations.getAddress());
                threadedConversations.setContact_name(contactName);
                threadedConversations.setSnippet(cursor.getString(snippetIndex));
                threadedConversations.setDate(cursor.getString(dateIndex));
                threadedConversations.setThread_id(cursor.getString(threadIdIndex));
                threadedConversations.setType(cursor.getInt(typeIndex));
//                threadedConversations.setIs_archived(cursor.getInt(isArchivedIndex)==1);
                threadedConversationsList.add(threadedConversations);
            } while(cursor.moveToNext());
            cursor.close();
        }

        return new LoadResult.Page<>(threadedConversationsList,
                null,
                null,
//                loadParams.getKey() != null ? loadParams.getKey() + 1 : null,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }

}
