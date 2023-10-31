package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afkanerd.deku.Router.GatewayServers.GatewayServer;

import java.util.List;

@Dao
public interface ThreadedConversationsDao {

    @Query("SELECT * FROM ThreadedConversations")
    LiveData<List<ThreadedConversations>> getAll();

    @Query("SELECT * FROM ThreadedConversations WHERE is_archived = 0")
    LiveData<List<ThreadedConversations>> getAllWithoutArchived();

    @Query("SELECT * FROM ThreadedConversations WHERE thread_id =:thread_id")
    LiveData<ThreadedConversations> get(long thread_id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ThreadedConversations threadedConversations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insert(List<ThreadedConversations> threadedConversationsList);

    @Update
    void update(ThreadedConversations threadedConversations);

    @Delete
    void delete(ThreadedConversations threadedConversations);
}
