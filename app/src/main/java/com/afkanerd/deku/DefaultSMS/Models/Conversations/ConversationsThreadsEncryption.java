package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
}
