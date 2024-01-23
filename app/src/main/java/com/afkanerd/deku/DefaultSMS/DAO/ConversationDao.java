package com.afkanerd.deku.DefaultSMS.DAO;

import android.provider.Telephony;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import java.util.List;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM Conversation WHERE thread_id =:thread_id AND type IS NOT 3 ORDER BY date DESC")
    PagingSource<Integer, Conversation> get(String thread_id);

    @Query("SELECT * FROM Conversation WHERE thread_id =:thread_id AND type IS NOT 3 ORDER BY date DESC")
    List<Conversation> getDefault(String thread_id);


    @Query("SELECT * FROM Conversation WHERE address =:address ORDER BY date DESC")
    PagingSource<Integer, Conversation> getByAddress(String address);

    @Query("SELECT * FROM Conversation WHERE thread_id =:thread_id ORDER BY date DESC")
    List<Conversation> getAll(String thread_id);

    @Query("SELECT * FROM ( SELECT * FROM Conversation GROUP BY thread_id HAVING MAX(date)) AS " +
            "latest_items WHERE thread_id IS NOT NULL ORDER BY date DESC")
    List<Conversation> getForThreading();

    @Query("SELECT * FROM Conversation ORDER BY date DESC")
    List<Conversation> getComplete();

    @Query("SELECT * FROM Conversation WHERE type = :type AND thread_id = :threadId ORDER BY date DESC")
    Conversation fetchTypedConversation(int type, String threadId);

//    @Query("SELECT * FROM Conversation WHERE body " +
//            "LIKE '%' || :text || '%' ORDER BY date DESC")
//    PagingSource<Integer, Conversation> find(String text);

    @Query("SELECT * FROM Conversation WHERE message_id =:message_id")
    Conversation getMessage(String message_id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Conversation conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Conversation> conversationList);

    @Update
    int update(Conversation conversation);

    @Update
    int update(List<Conversation> conversations);

    @Query("DELETE FROM Conversation WHERE thread_id = :threadId")
    int delete(String threadId);

    @Query("DELETE FROM Conversation WHERE thread_id IN (:threadIds)")
    void deleteAll(List<String> threadIds);

    @Query("DELETE FROM Conversation WHERE type = :type")
    int deleteAllType(int type);

    @Query("DELETE FROM Conversation WHERE type = :type AND thread_id = :thread_id")
    int deleteAllType(int type, String thread_id);
    @Delete
    int delete(Conversation conversation);

    @Delete
    int delete(List<Conversation> conversation);
}
