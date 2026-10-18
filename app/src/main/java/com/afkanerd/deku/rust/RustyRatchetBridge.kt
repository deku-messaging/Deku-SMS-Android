package com.afkanerd.deku.rust

open class RatchetException(message: String?) : Exception(message)
class RatchetInitFailedForAlice(message: String?) : RatchetException(message)
class RatchetInitFailedForBob(message: String?) : RatchetException(message)
class ExceededMaxSkipMessages(message: String?) : RatchetException(message)
class FailedToDeserializeState(message: String?) : RatchetException(message)
class FailedToSerializeState(message: String?) : RatchetException(message)

object RustyRatchetBridge {

    init {
        System.loadLibrary("rusty_ratchet_bridge")
    }

    external fun newState(): ByteArray
    external fun initAlice(state: ByteArray, sk: ByteArray, bob_dh_public_key: ByteArray): ByteArray
    external fun initBob(state: ByteArray, sk: ByteArray, bob_keypair: ByteArray): ByteArray
    external fun encrypt(state: ByteArray, plaintextText: ByteArray, ad: ByteArray): Array<ByteArray>
    external fun decrypt(
        state: ByteArray,
        ciphertext: ByteArray,
        header: ByteArray,
        ad: ByteArray,
    ): Array<ByteArray>
}