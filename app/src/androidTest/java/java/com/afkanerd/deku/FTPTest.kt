package java.com.afkanerd.deku

import androidx.test.filters.SmallTest

@SmallTest
class FTPTest {

    @Before
    fun init() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.resources.openRawResource(R.raw.smtp)
        properties.load(inputStream)
        properties.put("mail.smtp.host", properties.getProperty("host"))
        properties.put("mail.smtp.port", properties.getProperty("port"))
        properties.put("mail.debug", "true");
    }
}