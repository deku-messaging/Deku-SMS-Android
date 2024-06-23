package com.afkanerd.deku.Router.Models

import android.content.Context
import android.util.Log
import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.startup.AppInitializer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import com.afkanerd.deku.DefaultSMS.BuildConfig
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Router.GatewayServers.GatewayServer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Properties
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.mail.Message
import javax.mail.MessagingException
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
        properties["mail.smtp.host"] = gatewayServer.smtp.smtp_host
        properties["mail.smtp.port"] = gatewayServer.smtp.smtp_port
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
        val internetAddresses = InternetAddress.parse(gatewayServer.smtp.smtp_recipient)
        val session = Session.getInstance(properties, null)
        val message: Message = MimeMessage(session)
        message.setFrom(InternetAddress(gatewayServer.smtp.smtp_from))
        message.setRecipients(Message.RecipientType.TO, internetAddresses)
        message.subject = gatewayServer.smtp.smtp_subject
        message.sentDate = Date()
        message.setText(body)
        Transport.send(message, gatewayServer.smtp.smtp_username, gatewayServer.smtp.smtp_password)
    }

    fun removeWorkForMessage(context: Context?, messageId: String) {
        val tag = getTagForMessages(messageId)
        val workManager = WorkManager.getInstance(context!!)
        workManager.cancelAllWorkByTag(tag)
    }

    fun removeWorkForGatewayServers(context: Context?, gatewayServerId: Long) {
        val tag = getTagForGatewayServers(gatewayServerId)
        val workManager = WorkManager.getInstance(context!!)
        workManager.cancelAllWorkByTag(tag)
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

    fun reverseState(context: Context, state: WorkInfo.State): String {
        return when (state) {
            WorkInfo.State.SUCCEEDED -> context.getString(R.string.gateway_server_routing_state_success)
            WorkInfo.State.ENQUEUED -> context.getString(R.string.gateway_server_routing_state_enqueued)
            WorkInfo.State.FAILED -> context.getString(R.string.gateway_server_routing_state_failed)
            WorkInfo.State.RUNNING -> context.getString(R.string.gateway_server_routing_state_running)
            WorkInfo.State.CANCELLED -> context.getString(R.string.gateway_server_routing_state_cancelled)
            else -> ""
        }
    }

    fun getMessageIdsFromWorkManagers(context: Context): LiveData<List<WorkInfo>> {
//        AppInitializer.getInstance(context)
//                .initializeComponent(com.afkanerd.deku.WorkManagerInitializer::class.java)
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfosByTagLiveData(TAG_NAME_GATEWAY_SERVER)
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
