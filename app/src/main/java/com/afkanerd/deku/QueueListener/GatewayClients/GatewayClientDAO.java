package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GatewayClientDAO {

    @Query("SELECT * FROM GatewayClient")
    List<GatewayClient> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClient gatewayClient);

    @Delete
    int delete(GatewayClient gatewayClient);

    @Query("SELECT * FROM GatewayClient WHERE id=:id")
    GatewayClient fetch(long id);

//    @Query("UPDATE GatewayClient SET projectName=:projectName, projectBinding=:projectBinding WHERE id=:id")
//    void updateProjectNameAndProjectBinding(String projectName, String projectBinding, int id);

    @Update
    void update(GatewayClient gatewayClient);
}
