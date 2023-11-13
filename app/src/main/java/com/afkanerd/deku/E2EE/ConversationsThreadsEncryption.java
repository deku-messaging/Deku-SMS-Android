package com.afkanerd.deku.E2EE;

public class ConversationsThreadsEncryption {

    private int id;
    private int keyExchangeCount;

    private String publicKey;

    private String keystoreAlias;

    private long exchangeDate;

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getKeyExchangeCount() {
        return keyExchangeCount;
    }

    public void setKeyExchangeCount(int keyExchangeCount) {
        this.keyExchangeCount = keyExchangeCount;
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
