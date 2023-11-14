package com.afkanerd.deku.E2EE.Security;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomKeyStoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CustomKeyStore customKeyStore);

    @Query("SELECT * FROM CustomKeyStore WHERE keystoreAlias = :keystoreAlias LIMIT 1")
    CustomKeyStore find(String keystoreAlias);

    @Query("DELETE FROM CustomKeyStore WHERE keystoreAlias = :keystoreAlias")
    int delete(String keystoreAlias);

    @Query("SELECT * FROM CustomKeyStore")
    List<CustomKeyStore> getAll();
}
