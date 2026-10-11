package com.afkanerd.deku.Forwarder.data.models

import android.util.Log
import com.afkanerd.deku.Forwarder.extensions.toSha256
import com.afkanerd.smswithoutborders_libsmsmms.data.data.models.SmsMmsNatives
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
@Serializable
data class RouterItem(
    val thread_id: Int,
    val address: String,
    val dialing_code: String,
    val date: Long,
    val date_sent: Long,
    val read: Int,
    val status: Int,
    val type: Int,
    val body: String,
    val text: String,
    val sub_id: Long,
    val url: String? = null,
    var tag: String? = null
) {
    constructor(sms: SmsMmsNatives.Sms, isHashIncoming: Boolean) : this(
        thread_id = sms.thread_id,
        address = if(isHashIncoming) sms.address!!.toSha256() else sms.address!!,
        date = sms.date,
        date_sent = sms.date_sent,
        read = sms.read,
        status = sms.status,
        type = sms.type,
        body = sms.body!!,
        text = sms.body!!,
        sub_id = sms.sub_id,
        dialing_code = try {
            PhoneNumberUtil
                .getInstance()
                .parse(sms.address!!, null)
                .countryCode.toString()
        } catch(e: Exception) {
            e.printStackTrace()
            Log.e("RouterItem", "Failed to get dialing code: ${sms.address!!}")
            throw e
        }
    )

    fun serializeJson(): String =
        Json { prettyPrint = true }.encodeToString(this)
}
