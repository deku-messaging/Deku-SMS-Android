package com.afkanerd.deku.rust

object RustyRatchetBridge {

    init {
        System.loadLibrary("rusty_ratchet_bridge")
    }

    external fun newState(): Long
    external fun initAlice(handle: Long, sk: ByteArray, bob_dh_public_key: ByteArray): Long
}