package com.example.swob_deku.Models.GatewayServer;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GatewayServerDAO {
    @Query("SELECT * FROM GatewayServer")
    LiveData<List<GatewayServer>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayServer gatewayServer);
}
