package com.afkanerd.deku.Forwarder.data.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class GatewayServer(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @Embedded
    var smtp: SMTP? = null,

    @Embedded
    var ftp: FTP = FTP(),

    @ColumnInfo(name = "URL")
    var URL: String? = null,

    @ColumnInfo(name = "protocol")
    var protocol: String? = POST_PROTOCOL,

    @ColumnInfo(name = "tag")
    var tag: String = "",

    @ColumnInfo(name = "format")
    var format: String? = ALL_FORMAT,

    @ColumnInfo(name = "date")
    var date: Long? = null

) {
    companion object {
        @JvmField
        var BASE64_FORMAT: String = "base_64"
        var ALL_FORMAT: String = "all"
        @JvmField
        var POST_PROTOCOL: String = "HTTPS"
        var GATEWAY_SERVER_ID: String = "GATEWAY_SERVER_ID"
    }
}