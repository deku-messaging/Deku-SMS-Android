package com.example.swob_deku.Models.GatewayClients;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GatewayClientDAO {

    @Query("SELECT * FROM GatewayClient")
    LiveData<List<GatewayClient>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClient gatewayClient);

    @Delete
    int delete(GatewayClient gatewayClient);

    @Query("SELECT * FROM GatewayClient WHERE id=:id")
    GatewayClient fetch(int id);

    @Query("UPDATE GatewayClient SET projectName=:projectName, projectBinding=:projectBinding WHERE id=:id")
    void updateProjectNameAndProjectBinding(String projectName, String projectBinding, int id);
}
