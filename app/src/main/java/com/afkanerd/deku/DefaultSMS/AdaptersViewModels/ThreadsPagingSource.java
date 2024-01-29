package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;

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
        Cursor cursor = context.getContentResolver().query(
                Telephony.Threads.CONTENT_URI,
                null,
                null,
                null,
                "date DESC"
        );


        List<ThreadedConversations> threadedConversationsList = new ArrayList<>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ThreadedConversations tc = new ThreadedConversations();
                ThreadedConversationsDao threadedConversationsDao = tc.getDaoInstance(context);
                List<ThreadedConversations> threadedDraftsList =
                        threadedConversationsDao.getThreadedDraftsList(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT);
                tc.close();
                List<String> threadIds = new ArrayList<>();
                for(ThreadedConversations threadedConversations : threadedDraftsList)
                    threadIds.add(threadedConversations.getThread_id());
                Log.d(getClass().getName(), "# drafts: " + threadedDraftsList.size());

                if(cursor != null && cursor.moveToFirst()) {
                    do {
                        int recipientIdIndex = cursor.getColumnIndex("address");
                        int snippetIndex = cursor.getColumnIndex("body");
                        int dateIndex = cursor.getColumnIndex("date");
                        int threadIdIndex = cursor.getColumnIndex("thread_id");
                        int typeIndex = cursor.getColumnIndex("type");
                        int readIndex = cursor.getColumnIndex("read");

                        ThreadedConversations threadedConversations = new ThreadedConversations();
                        threadedConversations.setAddress(cursor.getString(recipientIdIndex));
                        if(threadedConversations.getAddress() == null || threadedConversations.getAddress().isEmpty())
                            continue;
                        threadedConversations.setThread_id(cursor.getString(threadIdIndex));
                        if(threadIds.contains(threadedConversations.getThread_id())) {
                            threadedConversations.setSnippet(threadedDraftsList.get(threadIds
                                            .indexOf(threadedConversations.getThread_id()))
                                    .getSnippet());
                            threadedConversations.setType(threadedDraftsList.get(threadIds
                                            .indexOf(threadedConversations.getThread_id()))
                                    .getType());
                        }
                        else {
                            threadedConversations.setSnippet(cursor.getString(snippetIndex));
                            threadedConversations.setType(cursor.getInt(typeIndex));
                        }
                        String contactName = Contacts.retrieveContactName(context,
                                threadedConversations.getAddress());
                        threadedConversations.setContact_name(contactName);
                        threadedConversations.setDate(cursor.getString(dateIndex));
                        threadedConversations.setType(cursor.getInt(typeIndex));
                        threadedConversations.setIs_read(cursor.getInt(readIndex) == 1);
                        threadedConversationsList.add(threadedConversations);
                    } while(cursor.moveToNext());
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(cursor != null)
            cursor.close();

        return new LoadResult.Page<>(threadedConversationsList,
                null,
                null,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }

}
