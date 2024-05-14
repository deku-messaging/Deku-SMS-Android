package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.lifecycle.LiveData;
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

    @Query("SELECT * FROM GatewayClient")
    LiveData<List<GatewayClient>> fetch();

    @Query("SELECT * FROM GatewayClient WHERE activated = 1")
    List<GatewayClient> fetchActivated();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClient gatewayClient);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<GatewayClient> gatewayClients);

    @Delete
    int delete(GatewayClient gatewayClient);

    @Delete
    void delete(List<GatewayClient> gatewayClients);

    @Query("DELETE FROM GatewayClient")
    void deleteAll();

    @Query("SELECT * FROM GatewayClient WHERE id=:id")
    GatewayClient fetch(long id);

    @Query("SELECT * FROM GatewayClient WHERE id=:id")
    LiveData<GatewayClient> fetchLiveData(long id);

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
