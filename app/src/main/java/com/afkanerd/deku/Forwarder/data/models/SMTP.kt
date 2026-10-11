package com.afkanerd.deku.Forwarder.data.models

data class SMTP (
    var smtp_host: String,
    var smtp_username: String,
    var smtp_password: String,
    var smtp_recipient: String,
    var smtp_from: String,
    var smtp_subject: String,
    var smtp_port: Int = 587,
) {
    companion object {
        const val PROTOCOL = "SMTP"
    }
}
