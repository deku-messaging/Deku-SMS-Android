package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import kotlin.coroutines.Continuation;


public class ConversationPagingSource {

    String threadId;
    ConversationDao conversationDao;
    public PagingSource<Integer, Conversation> pagingSource;
    public Pager<Integer, Conversation> getRoomPaging(
            ConversationDao conversationDao, String threadId, Integer initialKey) {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        int pageSize = 20;
        int prefetchDistance = 3 * pageSize;
        boolean enablePlaceholder = true;
        int initialLoadSize = 2 * pageSize;
        int maxSize = PagingConfig.MAX_SIZE_UNBOUNDED;

        pagingSource = conversationDao.get(threadId);
        return new Pager<>(new PagingConfig(
                pageSize,
                prefetchDistance,
                enablePlaceholder,
                initialLoadSize,
                maxSize
        ), initialKey, this::getPagingSource);
    }

    public PagingSource<Integer, Conversation> getPagingSource(){
        pagingSource = this.conversationDao.get(threadId);
        return pagingSource;
    }
}

//public class ConversationPagingSource extends PagingSource<Integer, Conversation> {
//
//    ConversationDao conversationDao;
//    String threadId;
//
//    Integer initialKey = 0;
//    public ConversationPagingSource(ConversationDao conversationDao, String threadId, Integer initialKey) {
//        this.threadId = threadId;
//        this.initialKey = initialKey;
//        this.conversationDao = conversationDao;
//    }
//
//
//    @Nullable
//    @Override
//    public LoadResult<Integer, Conversation> load(@NonNull LoadParams<Integer> loadParams, @NonNull Continuation<? super LoadResult<Integer, Conversation>> continuation) {
//        return new LoadResult.Page<>(conversationDao.getAllWithOffset(threadId, initialKey),
//                loadParams.getKey() != null ? loadParams.getKey() + 1 : null,
//                loadParams.getKey() != null ? loadParams.getKey() - 1 : null,
//                LoadResult.Page.COUNT_UNDEFINED,
//                LoadResult.Page.COUNT_UNDEFINED);
//    }
//
//    @Nullable
//    @Override
//    public Integer getRefreshKey(@NonNull PagingState<Integer, Conversation> pagingState) {
//        return null;
//    }
//}
