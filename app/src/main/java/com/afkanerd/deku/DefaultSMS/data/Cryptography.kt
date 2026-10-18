package com.afkanerd.deku.DefaultSMS.data

import java.security.SecureRandom

object Cryptography {

    fun generateSecureRandom(size: Int) : ByteArray {
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(size)
        secureRandom.nextBytes(randomBytes)
        return randomBytes
    }
}