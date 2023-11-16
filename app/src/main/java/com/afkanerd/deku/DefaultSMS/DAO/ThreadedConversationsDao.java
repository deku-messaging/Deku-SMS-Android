package com.afkanerd.deku.DefaultSMS.DAO;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;

import java.util.List;

@Dao
public interface ThreadedConversationsDao {

    @Query("SELECT * FROM ThreadedConversations")
    LiveData<List<ThreadedConversations>> getAllLiveData();

    @Query("SELECT * FROM ThreadedConversations ORDER BY date DESC")
    List<ThreadedConversations> getAll();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 1 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllWithoutArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 AND thread_id IN " +
            "(SELECT thread_id FROM Conversation WHERE is_archived = 1) " +
            "ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllEncrypted();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 AND thread_id NOT IN " +
            "(SELECT thread_id FROM Conversation WHERE is_archived = 1) " +
            "ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllNotEncrypted();

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id =:thread_id")
    ThreadedConversations get(String thread_id);

    @Query("SELECT * FROM ThreadedConversations WHERE address =:address")
    ThreadedConversations getByAddress(String address);

    @Query("SELECT Conversation.* FROM Conversation, ThreadedConversations WHERE text " +
            "LIKE '%' || :search_string || '%' AND Conversation.thread_id = ThreadedConversations.thread_id " +
            "GROUP BY Conversation.thread_id ORDER BY date DESC")
    List<Conversation> find(String search_string );

    @Query("SELECT * FROM Conversation WHERE thread_id =:thread_id AND text " +
            "LIKE '%' || :search_string || '%' GROUP BY thread_id ORDER BY date DESC")
    List<Conversation> findByThread(String search_string, String thread_id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ThreadedConversations threadedConversations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ThreadedConversations> threadedConversationsList);

    @Update
    int update(ThreadedConversations threadedConversations);

    @Delete
    void delete(ThreadedConversations threadedConversations);

    @Delete
    void delete(List<ThreadedConversations> threadedConversations);

    @Update(entity = ThreadedConversations.class)
    void archive(List<Archive> archiveList);

    @Update(entity = ThreadedConversations.class)
    void unarchive(List<Archive> archiveList);
}
