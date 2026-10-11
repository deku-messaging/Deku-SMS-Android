package com.afkanerd.deku.Forwarder.extensions

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

fun String.toSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = this.toByteArray(UTF_8)
    val hashBytes = digest.digest(bytes)
    return hashBytes.joinToString("") { "%02x".format(it) }
}