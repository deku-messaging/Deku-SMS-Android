package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ThreadedConversationsDao {

    @Query("SELECT * FROM ThreadedConversations")
    LiveData<List<ThreadedConversations>> getAll();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllWithoutArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id =:thread_id")
    ThreadedConversations get(String thread_id);

    @Query("SELECT * FROM Conversation WHERE body " +
            "LIKE '%' || :search_string || '%' GROUP BY thread_id ORDER BY date DESC")
    List<Conversation> find(String search_string );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ThreadedConversations threadedConversations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ThreadedConversations> threadedConversationsList);

    @Update
    int update(ThreadedConversations threadedConversations);

    @Delete
    void delete(ThreadedConversations threadedConversations);
}
