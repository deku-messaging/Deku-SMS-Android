package com.afkanerd.deku.DefaultSMS.DAO;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.afkanerd.deku.DefaultSMS.Models.Archive.Archive;

import java.util.List;

@Dao
public interface ArchiveDAO {

    @Query("SELECT * FROM Archive WHERE threadId=:threadId")
    Archive fetch(long threadId);

    @Query("SELECT * FROM Archive")
    List<Archive> fetchAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Archive archive);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Archive[] archive);

    @Delete
    void remove(Archive archive);

    @Delete
    void remove(Archive[] archive);
}
