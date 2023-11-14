package com.afkanerd.deku.E2EE.Security;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Entity(indices = {@Index(value={"keystoreAlias"}, unique=true)})
public class CustomKeyStore {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long date;
    private String keystoreAlias;

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

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
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
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        PrivateKey keystorePrivateKey = SecurityECDH.getPrivateKeyFromKeystore(this.keystoreAlias);
        byte[] decodedPrivateKey = Base64.decode(this.privateKey, Base64.DEFAULT);
        byte[] privateKey = SecurityRSA.decrypt(keystorePrivateKey, decodedPrivateKey);
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
