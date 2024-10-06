package com.afkanerd.deku.DefaultSMS.DAO;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationsThreadsEncryption;

import java.util.List;

@Dao
public interface ConversationsThreadsEncryptionDao {

    @Query("SELECT * FROM ConversationsThreadsEncryption WHERE keystoreAlias = :keystoreAlias")
    LiveData<ConversationsThreadsEncryption> fetchLiveData(String keystoreAlias);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ConversationsThreadsEncryption conversationsThreadsEncryption);

    @Update
    int update(ConversationsThreadsEncryption conversationsThreadsEncryption);

    @Query("SELECT * FROM ConversationsThreadsEncryption WHERE keystoreAlias = :keystoreAlias")
    ConversationsThreadsEncryption findByKeystoreAlias(String keystoreAlias);

    @Query("SELECT * FROM ConversationsThreadsEncryption")
    List<ConversationsThreadsEncryption> getAll();

    @Query("SELECT * FROM ConversationsThreadsEncryption WHERE keystoreAlias = :keystoreAlias")
    ConversationsThreadsEncryption fetch(String keystoreAlias);

    @Query("DELETE FROM ConversationsThreadsEncryption WHERE keystoreAlias = :keystoreAlias")
    int delete(String keystoreAlias);
}
