package com.afkanerd.deku.DefaultSMS.DAO;

import android.provider.Telephony;
import android.util.Base64;

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

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 AND is_read = 1 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllUnreadWithoutArchived();

    @Query("SELECT COUNT(Conversation.id) FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.thread_id = ThreadedConversations.thread_id AND " +
            "is_archived = 0 AND is_read = 0")
    int getAllUnreadWithoutArchivedCount();

    @Query("SELECT COUNT(ConversationsThreadsEncryption.id) FROM ConversationsThreadsEncryption")
    int getAllEncryptedCount();

    @Query("SELECT Conversation.address, " +
            "Conversation.text as snippet, " +
            "Conversation.thread_id, " +
            "Conversation.date, Conversation.type, Conversation.read, " +
            "ThreadedConversations.msg_count, ThreadedConversations.is_archived, " +
            "ThreadedConversations.is_blocked, ThreadedConversations.is_read, " +
            "ThreadedConversations.is_shortcode, ThreadedConversations.contact_name " +
            "FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.type = :type AND ThreadedConversations.thread_id = Conversation.thread_id " +
            "ORDER BY Conversation.date DESC")
    PagingSource<Integer, ThreadedConversations> getThreadedDrafts(int type);

    @Query("SELECT Conversation.address, " +
            "Conversation.text as snippet, " +
            "Conversation.thread_id, " +
            "Conversation.date, Conversation.type, Conversation.read, " +
            "0 as msg_count, ThreadedConversations.is_archived, ThreadedConversations.is_blocked, " +
            "ThreadedConversations.is_read, ThreadedConversations.is_shortcode " +
            "FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.type = :type AND ThreadedConversations.thread_id = Conversation.thread_id " +
            "ORDER BY Conversation.date DESC")
    List<ThreadedConversations> getThreadedDraftsList(int type);

    @Query("SELECT COUNT(ThreadedConversations.thread_id) " +
            "FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.type = :type AND ThreadedConversations.thread_id = Conversation.thread_id " +
            "ORDER BY Conversation.date DESC")
    int getThreadedDraftsListCount(int type);

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id =:thread_id")
    ThreadedConversations get(String thread_id);

    @Query("SELECT * FROM ThreadedConversations WHERE address =:address")
    ThreadedConversations getByAddress(String address);

    @Query("SELECT * FROM ThreadedConversations WHERE address IN(:addresses) AND is_archived = 0 " +
            "ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getByAddress(List<String> addresses);

    @Query("SELECT * FROM ThreadedConversations WHERE address NOT IN(:addresses)")
    PagingSource<Integer, ThreadedConversations> getNotInAddress(List<String> addresses);

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id IN (:threadedConversationsList)")
    List<ThreadedConversations> find(List<String> threadedConversationsList);

    @Query("SELECT Conversation.* FROM Conversation, ThreadedConversations WHERE text " +
            "LIKE '%' || :search_string || '%' AND Conversation.thread_id = ThreadedConversations.thread_id " +
            "GROUP BY ThreadedConversations.thread_id ORDER BY date DESC")
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

    @Query("DELETE FROM threadedconversations")
    void deleteAll();

    @Update(entity = ThreadedConversations.class)
    void archive(List<Archive> archiveList);

    @Update(entity = ThreadedConversations.class)
    void unarchive(List<Archive> archiveList);
}
