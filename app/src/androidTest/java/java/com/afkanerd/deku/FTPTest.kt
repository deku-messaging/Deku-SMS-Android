package java.com.afkanerd.deku

import android.content.Context
import android.util.Log
import androidx.paging.LOG_TAG
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.afkanerd.deku.DefaultSMS.R
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.InetAddress
import java.nio.charset.Charset
import java.util.Properties


@SmallTest
class FTPTest {

    val properties: Properties = Properties()
    lateinit var context: Context
    val ftpClient = FTPSClient("TLS")
//    val ftpClient = FTPSClient()
//    val ftpClient = FTPClient()

    @Before
    fun init() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.resources.openRawResource(R.raw.ftp)
        properties.load(inputStream)

        ftpClient.setConnectTimeout(10000); // Set connection timeout

    }

    @Test
    fun ftpConnection() {
//        val host = properties.getProperty("host") + ":" + properties.getProperty("port")
        val host = properties.getProperty("host")
        ftpClient.connect(InetAddress.getByName(host));
        ftpClient.login(properties.getProperty("username"), properties.getProperty("password"));
//        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

//         TODO: this should become a configuration
        ftpClient.enterLocalPassiveMode()

        if(ftpClient.replyCode in 200..299) {
            // ... connect and login logic ...
            // TODO: this should become a configuration
            ftpClient.execPBSZ(0); // Set protection buffer size (optional)
            ftpClient.execPROT("P"); // Set protection mode to private (optional)
//            if(!ftpClient.changeWorkingDirectory(properties.getProperty("directory"))) {
//                ftpClient.makeDirectory(properties.getProperty("directory"))
//                ftpClient.changeWorkingDirectory(properties.getProperty("directory"))
//            }
            val body = saveToJson()
            val filename = System.currentTimeMillis().toString() + ".json"
            Log.d(LOG_TAG, "FTP Connected: " + ftpClient.isConnected + ":" + filename)
//            val stored = ftpClient.storeFile(properties.getProperty("remotePath") + filename,
//                    body.byteInputStream(Charset.defaultCharset()))

            val files = ftpClient.listFiles()
            Log.d(LOG_TAG, "Store files: " + files.size)
            val stored = ftpClient.storeFile(filename, body.byteInputStream(Charset.defaultCharset()))
            Log.d(LOG_TAG, "Filed stored: $stored")
            ftpClient.disconnect();
            Log.d(LOG_TAG, "FTP Connected: " + ftpClient.isConnected)
        } else {
            throw Exception("Failed to connect to FTP server: " + ftpClient.replyCode)
        }
    }

    fun saveToJson(): String {
        val json = JSONObject()
        json.put("component1", "url")
        json.put("component2", "url")
        return json.toString()
    }

}