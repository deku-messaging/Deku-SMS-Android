package com.afkanerd.deku.DefaultSMS.Models

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityCurve25519
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityRSA
import java.nio.ByteBuffer


object E2EEHandler {
    private const val HYBRID_KEYS_FILE = "com.afkanerd.dekusms.HYBRID_KEYS_FILE"

    private fun secureStorePrivateKey(context: Context,
                                      keystoreAlias: String,
                                      encryptedCipherPrivateKey: ByteArray) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            HYBRID_KEYS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit()
            .putString(keystoreAlias, Base64.encodeToString(encryptedCipherPrivateKey,
                Base64.DEFAULT)).apply()
    }

    fun generateKey(context: Context, keystoreAlias: String): ByteArray {
        val libSigCurve25519 = SecurityCurve25519()
        val publicKey = libSigCurve25519.generateKey()
        val encryptionPublicKey = SecurityRSA.generateKeyPair(keystoreAlias, 2048)
        val privateKeyCipherText = SecurityRSA.encrypt(encryptionPublicKey,
            libSigCurve25519.privateKey)
        secureStorePrivateKey(context, keystoreAlias, privateKeyCipherText)
        return publicKey
    }

    private fun getSecuredStoredPrivateKey(context: Context, keystoreAlias: String) : String {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            HYBRID_KEYS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString(keystoreAlias, "")!!
    }


    private fun fetchPrivateKey(context: Context, keystoreAlias: String) : ByteArray {
        val cipherPrivateKeyString = getSecuredStoredPrivateKey(context, keystoreAlias)
        if(cipherPrivateKeyString.isBlank()) {
            throw Exception("Cipher private key is empty...")
        }

        val cipherPrivateKey = Base64.decode(cipherPrivateKeyString, Base64.DEFAULT)
        val keypair = KeystoreHelpers.getKeyPairFromKeystore(keystoreAlias)
        return SecurityRSA.decrypt(keypair.private, cipherPrivateKey)
    }

    fun calculateSharedSecret(context: Context, keystoreAlias: String, publicKey: ByteArray): ByteArray {
        val privateKey = fetchPrivateKey(context, keystoreAlias)
        val libSigCurve25519 = SecurityCurve25519(privateKey)
        return libSigCurve25519.calculateSharedSecret(publicKey)
    }

    fun formatRequestPublicKey(publicKey: ByteArray) : ByteArray {
        val mn = ByteArray(4)
        val lenPubKey = ByteArray(4)

        ByteBuffer.wrap(mn).putInt(0)
        ByteBuffer.wrap(lenPubKey).putInt(publicKey.size)

        return mn + lenPubKey + publicKey
    }
}