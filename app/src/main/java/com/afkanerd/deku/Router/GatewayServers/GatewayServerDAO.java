package com.afkanerd.deku.Router.GatewayServers;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GatewayServerDAO {
    @Query("SELECT * FROM GatewayServer")
    LiveData<List<GatewayServer>> getAll();

    @Query("SELECT * FROM GatewayServer")
    List<GatewayServer> getAllList();

    @Query("SELECT * FROM GatewayServer WHERE id=:id")
    GatewayServer get(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayServer gatewayServer);

    @Update
    void update(GatewayServer gatewayServer);

    @Delete
    void delete(GatewayServer gatewayServer);
}
