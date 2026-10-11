package com.afkanerd.deku.Forwarder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import kotlinx.coroutines.flow.Flow

@Dao
interface GatewayServerDAO {
    @get:Query("SELECT * FROM GatewayServer")
    val all: Flow<MutableList<GatewayServer>>

    @Query("SELECT * FROM GatewayServer WHERE id IN (:gatewayServerIds)")
    fun fetch(gatewayServerIds: List<String>): List<GatewayServer>?

    @get:Query("SELECT * FROM GatewayServer")
    val allList: List<GatewayServer>?

    @Query("SELECT * FROM GatewayServer WHERE id=:id")
    fun get(id: String?): GatewayServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(gatewayServer: GatewayServer): Long

    @Update
    fun update(gatewayServer: GatewayServer)

    @Delete
    fun delete(gatewayServer: GatewayServer)
}
