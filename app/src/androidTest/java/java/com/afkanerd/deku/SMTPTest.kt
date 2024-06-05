package java.com.afkanerd.deku

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import org.junit.Before
import org.junit.Test
import java.util.Properties
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Router.Models.RouterItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.MimeMessage


@SmallTest
class SMTPTest {
    val properties: Properties = Properties()
    lateinit var context: Context

    @Before
    fun init() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.resources.openRawResource(R.raw.smtp)
        properties.load(inputStream)
        properties.put("mail.smtp.host", properties.getProperty("host"))
        properties.put("mail.smtp.port", properties.getProperty("port"))
        properties.put("mail.debug", "true");

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true")
    }


    @Test
    fun smtpTest() {

        val session = Session.getInstance(properties, null)
        with(session) {
            val msg = MimeMessage(session)
            msg.setFrom(properties.getProperty("username"))
            msg.setRecipients(Message.RecipientType.TO,
                "developers@dekusms.com")
            msg.subject= "Deku Development"
            msg.sentDate= Date()
            msg.setText("Hi Deku devs,\nHere is our first sample mail from Android studio\n" +
                    "Thanks\n" + Date())
            Transport.send(msg,
                properties.getProperty("username"), properties.getProperty("password"))
        }
    }

    @Test
    fun testJson() {
        val conversation = Conversation()
        conversation.address = "test_address"
        conversation.id = 1
        conversation.text = "hello world"

        val routerItem = RouterItem(conversation)
        routerItem.tag = "sample_tag"

        println(routerItem.serializeJson())
    }
}