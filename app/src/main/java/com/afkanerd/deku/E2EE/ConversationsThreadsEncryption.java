package com.afkanerd.deku.E2EE;

import android.content.Context;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;
import com.afkanerd.deku.E2EE.Security.CustomKeyStoreDao;

@Entity(indices = {@Index(value={"keystoreAlias"}, unique=true)})
public class ConversationsThreadsEncryption {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // DHs comes from here
    private String keystoreAlias;

    // DHr comes from here
    private String publicKey;

    private String states;

    // would most likely use this for backward compatibility
    private long exchangeDate;

    public void setStates(String states) {
        this.states = states;
    }

    public String getStates() {
        return this.states;
    }

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getExchangeDate() {
        return exchangeDate;
    }

    public void setExchangeDate(long exchangeDate) {
        this.exchangeDate = exchangeDate;
    }

    @Ignore
    Datastore databaseConnector;
    public ConversationsThreadsEncryptionDao getDaoInstance(Context context) {
        databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration8To9())
                .enableMultiInstanceInvalidation()
                .build();
        return databaseConnector.conversationsThreadsEncryptionDao();
    }

    public void close() {
        if(databaseConnector != null)
            databaseConnector.close();
    }
}
