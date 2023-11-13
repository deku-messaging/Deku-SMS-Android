package com.afkanerd.deku.E2EE.Security;

import android.content.Context;
import android.util.Base64;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;
import com.afkanerd.deku.E2EE.E2EEHandler;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

@Entity
public class CustomKeyStore {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long date;
    private String keyStoreAlias;

    private String publicKey;

    private String privateKey;

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public PrivateKey buildPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException,
            UnrecoverableKeyException, CertificateException, KeyStoreException, IOException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        PrivateKey keystorePrivateKey = SecurityECDH.getPrivateKeyFromKeystore(this.keyStoreAlias);
        byte[] decodePrivateKey = Base64.decode(this.privateKey, Base64.DEFAULT);
        byte[] privateKey = SecurityRSA.decrypt(keystorePrivateKey, decodePrivateKey);
        return SecurityECDH.buildPrivateKey(privateKey);
    }

    public static CustomKeyStoreDao getDao(Context context) {
        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration8To9())
                .build();
        CustomKeyStoreDao customKeyStoreDao = databaseConnector.customKeyStoreDao();
        databaseConnector.close();
        return customKeyStoreDao;
    }
}
