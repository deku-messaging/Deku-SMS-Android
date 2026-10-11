package com.afkanerd.deku.Forwarder.data

import android.util.Log
import android.util.Pair
import androidx.work.WorkInfo
import com.afkanerd.deku.DefaultSMS.BuildConfig
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPSClient
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object RouterHandler {
    fun routeFTPMessages(body: String, gatewayServer: GatewayServer) {
        Log.d(RouterHandler::class.java.getName(), "Request to route - FTP: $body")

        // TODO: move TLS into a parameter configuration
        val ftpsClient = FTPSClient("TLS")
        ftpsClient.connect(gatewayServer.ftp.ftp_host)
        ftpsClient.login(gatewayServer.ftp.ftp_username, gatewayServer.ftp.ftp_password)
        ftpsClient.setFileType(FTP.BINARY_FILE_TYPE)
        ftpsClient.enterLocalPassiveMode()
        when(ftpsClient.replyCode) {
            in 200..300 -> {
                // TODO: move this to a parameter configuration
                ftpsClient.execPBSZ(0) // Set protection buffer size (optional)
                ftpsClient.execPROT("P") // Set protection mode to private (optional)

                if (gatewayServer.ftp.ftp_remote_path == null)
                    gatewayServer.ftp.ftp_remote_path =
                            System.currentTimeMillis().toString() + ".json"
                val stored = ftpsClient.storeFile(gatewayServer.ftp.ftp_remote_path,
                        ByteArrayInputStream(body.toByteArray(StandardCharsets.UTF_8)))
                ftpsClient.disconnect()
                if (!stored) {
                    throw Exception("Failed to write file to FTP server")
                }
            }
        }
    }

    fun routeSmtpMessages(body: String, gatewayServer: GatewayServer) {
        Log.d(javaClass.name, "Request to route - SMTP: $body")

        val properties = Properties()
        properties["mail.smtp.host"] = gatewayServer.smtp?.smtp_host
        properties["mail.smtp.port"] = gatewayServer.smtp?.smtp_port
        properties["mail.smtp.auth"] = "true";
        properties["mail.smtp.starttls.enable"] = "true"
        if (BuildConfig.DEBUG) properties["mail.debug"] = "true"

        /**
         * TODO
         * if (authenticationRequired) {
         * Authenticator auth = new SMTPAuthenticator();
         * props.put("mail.smtp.auth", "true");
         * session = Session.getDefaultInstance(props, auth);
         * } else {
         * session = Session.getDefaultInstance(props, null);
         * }
         */
        val internetAddresses = InternetAddress.parse(gatewayServer.smtp?.smtp_recipient)
        val session = Session.getInstance(properties, null)
        val message: Message = MimeMessage(session)
        message.setFrom(InternetAddress(gatewayServer.smtp?.smtp_from))
        message.setRecipients(Message.RecipientType.TO, internetAddresses)
        message.subject = gatewayServer.smtp?.smtp_subject
        message.sentDate = Date()
        message.setText(body)
        Transport.send(message, gatewayServer.smtp?.smtp_username,
            gatewayServer.smtp?.smtp_password)
    }

    const val TAG_NAME_GATEWAY_SERVER = "TAG_NAME_GATEWAY_SERVER"
    const val TAG_GATEWAY_SERVER_MESSAGE_ID = "TAG_GATEWAY_SERVER_MESSAGE_ID:"
    const val TAG_GATEWAY_SERVER_ID = "TAG_GATEWAY_SERVER_ID:"
    fun getTagForMessages(messageId: String): String {
        return TAG_GATEWAY_SERVER_MESSAGE_ID + messageId
    }

    private fun getMessageIdFromTag(tag: String): String {
        return tag.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
    }

    fun getTagForGatewayServers(gatewayServerId: Long): String {
        return TAG_GATEWAY_SERVER_ID + gatewayServerId
    }

    private fun getGatewayServerIdFromTag(tag: String): String {
        return tag.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
    }

    fun workInfoParser(workInfo: WorkInfo): Pair<String, String> {
        var messageId = ""
        var gatewayServerId = ""
        for (tag in workInfo.tags) {
            if (tag.contains(TAG_GATEWAY_SERVER_ID)) {
                gatewayServerId = getGatewayServerIdFromTag(tag)
            }
            if (tag.contains(TAG_GATEWAY_SERVER_MESSAGE_ID)) {
                messageId = getMessageIdFromTag(tag)
            }
        }
        return Pair(messageId, gatewayServerId)
    }
}