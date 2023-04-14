package com.example.swob_deku.Models.Archive;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.swob_deku.Models.GatewayServer.GatewayServer;
import com.example.swob_deku.Models.SMS.SMS;

import java.util.List;

@Dao
public interface ArchiveDAO {

    @Query("SELECT * FROM Archive")
    LiveData<List<GatewayServer>> getAll();

    @Query("SELECT * FROM Archive WHERE threadId=:threadId")
    Archive fetch(String threadId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Archive archive);
}
