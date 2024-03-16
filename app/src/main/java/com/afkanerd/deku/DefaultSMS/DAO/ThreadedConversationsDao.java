package com.afkanerd.deku.DefaultSMS.DAO;

import android.provider.Telephony;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.afkanerd.deku.DefaultSMS.Models.Archive;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;

import java.util.List;

@Dao
public interface ThreadedConversationsDao {

    @Query("SELECT * FROM ThreadedConversations ORDER BY date DESC")
    List<ThreadedConversations> getAll();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 1 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE is_blocked = 1 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getBlocked();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 AND is_blocked = 0 " +
            "ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllWithoutArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0 AND is_read = 0 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getAllUnreadWithoutArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE is_mute = 1 ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getMuted();

    @Query("SELECT COUNT(Conversation.id) FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.thread_id = ThreadedConversations.thread_id AND " +
            "is_archived = 0 AND read = 0")
    int getCountUnread();

    @Query("SELECT COUNT(Conversation.id) FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.thread_id = ThreadedConversations.thread_id AND " +
            "is_archived = 0 AND read = 0 AND ThreadedConversations.thread_id IN(:ids)")
    int getCountUnread(List<String> ids);

    @Query("SELECT COUNT(ConversationsThreadsEncryption.id) FROM ConversationsThreadsEncryption")
    int getCountEncrypted();

    @Query("SELECT COUNT(ThreadedConversations.thread_id) FROM ThreadedConversations " +
            "WHERE is_blocked = 1")
    int getCountBlocked();

    @Query("SELECT COUNT(ThreadedConversations.thread_id) FROM ThreadedConversations " +
            "WHERE is_mute = 1")
    int getCountMuted();

    @Query("SELECT Conversation.address, " +
            "Conversation.text as snippet, " +
            "Conversation.thread_id, " +
            "Conversation.date, Conversation.type, Conversation.read, " +
            "ThreadedConversations.msg_count, ThreadedConversations.is_archived, " +
            "ThreadedConversations.is_blocked, ThreadedConversations.is_read, " +
            "ThreadedConversations.is_shortcode, ThreadedConversations.contact_name, " +
            "ThreadedConversations.is_mute, ThreadedConversations.is_secured, " +
            "ThreadedConversations.isSelf " +
            "FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.type = :type AND ThreadedConversations.thread_id = Conversation.thread_id " +
            "ORDER BY Conversation.date DESC")
    PagingSource<Integer, ThreadedConversations> getThreadedDrafts(int type);

    @Query("SELECT COUNT(ThreadedConversations.thread_id) " +
            "FROM Conversation, ThreadedConversations WHERE " +
            "Conversation.type = :type AND ThreadedConversations.thread_id = Conversation.thread_id " +
            "ORDER BY Conversation.date DESC")
    int getThreadedDraftsListCount(int type);

    @Query("DELETE FROM ThreadedConversations WHERE ThreadedConversations.type = :type")
    int deleteForType(int type);

    @Query("DELETE FROM Conversation WHERE Conversation.type = :type")
    int clearConversationType(int type);

    @Transaction
    default void clearDrafts(int type) {
        clearConversationType(type);
        deleteForType(type);
    }

    @Query("UPDATE ThreadedConversations SET is_read = :read")
    int updateAllRead(int read);

    @Query("UPDATE Conversation SET read = :read")
    int updateAllReadConversation(int read);

    @Query("UPDATE ThreadedConversations SET is_read = :read WHERE thread_id IN(:ids)")
    int updateAllRead(int read, List<String> ids);

    @Query("UPDATE ThreadedConversations SET is_read = :read WHERE thread_id = :id")
    int updateAllRead(int read, long id);

    @Query("UPDATE ThreadedConversations SET is_mute = :muted WHERE thread_id = :id")
    int updateMuted(int muted, String id);

    @Query("UPDATE ThreadedConversations SET is_mute = :muted WHERE thread_id IN(:ids)")
    int updateMuted(int muted, List<String> ids);

    @Query("UPDATE ThreadedConversations SET is_mute = 0 WHERE is_mute = 1")
    int updateUnMuteAll();

    @Query("UPDATE Conversation SET read = :read WHERE thread_id IN(:ids)")
    int updateAllReadConversation(int read, List<String> ids);

    @Query("UPDATE Conversation SET read = :read WHERE thread_id = :id")
    int updateAllReadConversation(int read, long id);

    @Transaction
    default void updateRead(int read) {
        updateAllRead(read);
        updateAllReadConversation(read);
    }

    @Transaction
    default void updateRead(int read, List<String> ids) {
        updateAllRead(read, ids);
        updateAllReadConversation(read, ids);
    }

    @Transaction
    default void updateRead(int read, long id) {
        updateAllRead(read, id);
        updateAllReadConversation(read, id);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ThreadedConversations> threadedConversationsList);

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id =:thread_id")
    ThreadedConversations get(String thread_id);

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id IN (:threadIds)")
    List<ThreadedConversations> getList(List<String> threadIds);

    @Query("SELECT * FROM ThreadedConversations WHERE address =:address")
    ThreadedConversations getByAddress(String address);

    @Query("SELECT * FROM ThreadedConversations WHERE address IN(:addresses) AND is_archived = 0 " +
            "ORDER BY date DESC")
    PagingSource<Integer, ThreadedConversations> getByAddress(List<String> addresses);

    @Query("SELECT * FROM ThreadedConversations WHERE address NOT IN(:addresses)")
    PagingSource<Integer, ThreadedConversations> getNotInAddress(List<String> addresses);

    @Query("SELECT address FROM ThreadedConversations WHERE thread_id IN (:threadedConversationsList)")
    List<String> findAddresses(List<String> threadedConversationsList);

    @Query("SELECT Conversation.* FROM Conversation, ThreadedConversations WHERE text " +
            "LIKE '%' || :search_string || '%' AND Conversation.thread_id = ThreadedConversations.thread_id " +
            "GROUP BY ThreadedConversations.thread_id ORDER BY date DESC")
    List<Conversation> findAddresses(String search_string );

    @Query("SELECT * FROM Conversation WHERE thread_id =:thread_id AND text " +
            "LIKE '%' || :search_string || '%' GROUP BY thread_id ORDER BY date DESC")
    List<Conversation> findByThread(String search_string, String thread_id);

    @Insert
    long _insert(ThreadedConversations threadedConversations);

    @Transaction
    default ThreadedConversations insertThreadFromConversation(Conversation conversation) {
        /* - Import things are:
        1. Dates
        2. Snippet
        3. ThreadId
         */
        final String dates = conversation.getDate();
        final String snippet = conversation.getText();
        final String threadId = conversation.getThread_id();
        final String address = conversation.getAddress();

        final int type = conversation.getType();

        final boolean isRead = type != Telephony.Sms.MESSAGE_TYPE_INBOX || conversation.isRead();
        final boolean isSecured = conversation.isIs_encrypted();

        ThreadedConversations threadedConversations = Datastore.datastore.threadedConversationsDao()
                .get(conversation.getThread_id());
        threadedConversations.setDate(dates);
        threadedConversations.setSnippet(snippet);
        threadedConversations.setIs_read(isRead);
        threadedConversations.setIs_secured(isSecured);
        threadedConversations.setAddress(address);
        threadedConversations.setType(type);

        update(threadedConversations);
        threadedConversations = Datastore.datastore.threadedConversationsDao()
                .get(conversation.getThread_id());
        return threadedConversations;
    }
    @Transaction
    default ThreadedConversations insertThreadAndConversation(Conversation conversation) {
        /* - Import things are:
        1. Dates
        2. Snippet
        3. ThreadId
         */
        final String dates = conversation.getDate();
        final String snippet = conversation.getText();
        final String threadId = conversation.getThread_id();
        final String address = conversation.getAddress();

        final int type = conversation.getType();

        final boolean isRead = type != Telephony.Sms.MESSAGE_TYPE_INBOX || conversation.isRead();
        final boolean isSecured = conversation.isIs_encrypted();

        boolean insert = false;
        ThreadedConversations threadedConversations = get(threadId);
        if(threadedConversations == null) {
            threadedConversations = new ThreadedConversations();
            threadedConversations.setThread_id(threadId);
            insert = true;
        }
        threadedConversations.setDate(dates);
        threadedConversations.setSnippet(snippet);
        threadedConversations.setIs_read(isRead);
        threadedConversations.setIs_secured(isSecured);
        threadedConversations.setAddress(address);
        threadedConversations.setType(type);

        long id = Datastore.datastore.conversationDao()._insert(conversation);
        if(insert)
            _insert(threadedConversations);
        else {
            update(threadedConversations);
        }

        return threadedConversations;
    }

    @Update
    int _update(ThreadedConversations threadedConversations);

    @Transaction
    default long update(ThreadedConversations threadedConversations) {
        if(threadedConversations.getDate() == null || threadedConversations.getDate().isEmpty())
            threadedConversations.setDate(Datastore.datastore.conversationDao()
                    .fetchLatestForThread(threadedConversations.getThread_id()).getDate());
        return _update(threadedConversations);
    }

    @Delete
    void delete(ThreadedConversations threadedConversations);

//    @Delete
//    void delete(List<ThreadedConversations> threadedConversations);

    @Query("DELETE FROM ThreadedConversations WHERE thread_id IN(:ids)")
    void delete(List<String> ids);

    @Query("DELETE FROM threadedconversations")
    void deleteAll();

    @Update(entity = ThreadedConversations.class)
    void archive(List<Archive> archiveList);

    @Update(entity = ThreadedConversations.class)
    void unarchive(List<Archive> archiveList);
}
