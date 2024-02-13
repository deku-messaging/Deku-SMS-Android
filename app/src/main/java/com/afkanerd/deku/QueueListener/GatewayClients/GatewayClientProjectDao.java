package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GatewayClientProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<GatewayClientProjects> gatewayClientProjectsList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClientProjects gatewayClientProjects);

    @Query("SELECT * FROM GatewayClientProjects WHERE id = :id")
    GatewayClientProjects fetch(long id);

    @Query("SELECT * FROM GatewayClientProjects WHERE gatewayClientId = :gatewayClientId")
    LiveData<List<GatewayClientProjects>> fetchGatewayClientId(long gatewayClientId);

    @Update
    void update(GatewayClientProjects gatewayClientProjects);

    @Query("DELETE FROM GatewayClientProjects WHERE gatewayClientId = :id")
    void deleteGatewayClientId(long id);

}
