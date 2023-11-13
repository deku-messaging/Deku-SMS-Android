package com.afkanerd.deku.E2EE.Security;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface CustomKeyStoreDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CustomKeyStore customKeyStore);

    @Query("SELECT * FROM CustomKeyStore WHERE keyStoreAlias = :keystoreAlias")
    CustomKeyStore find(String keystoreAlias);
}
