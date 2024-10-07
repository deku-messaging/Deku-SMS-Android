package com.afkanerd.deku.DefaultSMS.Models

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.key
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityCurve25519
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityRSA
import java.nio.ByteBuffer
import java.nio.ByteOrder


object E2EEHandler {
    private const val HYBRID_KEYS_FILE = "com.afkanerd.dekusms.HYBRID_KEYS_FILE"

    enum class MagicNumber(val num: Int) {
        REQUEST(0),
        ACCEPT(1)
    }

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

    fun secureStorePeerPublicKey(context: Context, keystoreAlias: String,
                                         publicKey: ByteArray) {
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
            .putString(derivePeerPublicKeystoreAlias(keystoreAlias),
                Base64.encodeToString(publicKey, Base64.DEFAULT)).apply()
    }

    fun generateKey(context: Context, keystoreAlias: String, type: MagicNumber): ByteArray {
        val libSigCurve25519 = SecurityCurve25519()
        val publicKey = libSigCurve25519.generateKey()
        val encryptionPublicKey = SecurityRSA.generateKeyPair(keystoreAlias, 2048)
        val privateKeyCipherText = SecurityRSA.encrypt(encryptionPublicKey,
            libSigCurve25519.privateKey)

        when(type) {
            MagicNumber.REQUEST ->
                secureStorePrivateKey(context, deriveSecureRequestKeystoreAlias(keystoreAlias),
                    privateKeyCipherText)
            MagicNumber.ACCEPT ->
                secureStorePrivateKey(context, deriveSecureAcceptKeystoreAlias(keystoreAlias),
                    privateKeyCipherText)
        }
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
        val mn: ByteArray = byteArrayOf(MagicNumber.REQUEST.num.toByte())
        val lenPubKey = ByteArray(4)

        ByteBuffer.wrap(lenPubKey).order(ByteOrder.LITTLE_ENDIAN).putInt(publicKey.size)

        return mn + lenPubKey + publicKey
    }

    fun isValidPublicKey(data: ByteArray) : Boolean {
        val magicNumber: Int = data[0].toInt()

        val lenPubKey = ByteArray(4)
        System.arraycopy(data, 1, lenPubKey, 0, 4)
        val lenPublicKey = ByteBuffer.wrap(lenPubKey).order(ByteOrder.LITTLE_ENDIAN).getInt()

        // TODO: Can validate based on length
        val pubKey = ByteArray(lenPublicKey)
        System.arraycopy(data, 5, pubKey, 0, pubKey.size)

        // TODO: wild guess
        return (magicNumber == MagicNumber.REQUEST.num || magicNumber == MagicNumber.ACCEPT.num)
                && lenPublicKey % 4 == 0
    }

    private fun deriveSecureRequestKeystoreAlias(address: String) : String{
        return address + "_secure_request"
    }

    private fun deriveSecureAcceptKeystoreAlias(address: String) : String{
        return address + "_secure_accept"
    }

    private fun derivePeerPublicKeystoreAlias(address: String) : String{
        return address + "_peer_public_key"
    }

    fun getRequestType(data: ByteArray) : MagicNumber {
        val mn = ByteArray(4)
        System.arraycopy(data, 0, mn, 0, 4)
        val magicNumber = ByteBuffer.wrap(mn).order(ByteOrder.LITTLE_ENDIAN).getInt()

        return MagicNumber.valueOf(magicNumber.toString())
    }

    fun hasRequest(context: Context, address: String): Boolean {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            HYBRID_KEYS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).contains(deriveSecureRequestKeystoreAlias(address))
    }
}