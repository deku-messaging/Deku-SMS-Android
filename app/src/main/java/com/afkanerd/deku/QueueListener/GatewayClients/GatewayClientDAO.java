package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GatewayClientDAO {

    @Query("SELECT * FROM GatewayClient")
    List<GatewayClient> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClient gatewayClient);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<GatewayClient> gatewayClients);

    @Delete
    int delete(GatewayClient gatewayClient);

    @Delete
    void delete(List<GatewayClient> gatewayClients);

    @Query("SELECT * FROM GatewayClient WHERE id=:id")
    GatewayClient fetch(long id);

//    @Query("UPDATE GatewayClient SET projectName=:projectName, projectBinding=:projectBinding WHERE id=:id")
//    void updateProjectNameAndProjectBinding(String projectName, String projectBinding, int id);

    @Update
    void update(GatewayClient gatewayClient);

//    @Transaction
//    default void repentance(List<GatewayClient> sinFulGatewayClients, List<GatewayClient> afreshGatewayClient) {
//        delete(sinFulGatewayClients);
//        insert(afreshGatewayClient);
//    }
}
