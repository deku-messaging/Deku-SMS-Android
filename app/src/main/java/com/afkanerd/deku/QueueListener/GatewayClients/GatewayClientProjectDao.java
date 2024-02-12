package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GatewayClientProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<GatewayClientProjects> gatewayClientProjectsList);

    @Query("SELECT * FROM GatewayClientProjects WHERE gatewayClientId = :gatewayClientId")
    LiveData<List<GatewayClientProjects>> fetchGatewayClientId(long gatewayClientId);
}
