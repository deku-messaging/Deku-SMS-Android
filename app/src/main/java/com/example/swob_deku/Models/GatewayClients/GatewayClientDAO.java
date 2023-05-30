package com.example.swob_deku.Models.GatewayClients;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

@Dao
public interface GatewayClientDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayClient gatewayClient);
}
