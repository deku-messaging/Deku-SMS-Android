package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM Conversation")
    LiveData<List<Conversation>> getAll();

    @Query("SELECT * FROM Conversation WHERE message_id =:message_id")
    LiveData<Conversation> get(long message_id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Conversation conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Conversation> conversationList);

    @Update
    void update(Conversation conversation);

    @Delete
    void delete(Conversation conversation);
}
