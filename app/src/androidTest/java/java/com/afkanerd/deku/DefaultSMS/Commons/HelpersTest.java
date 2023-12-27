package java.com.afkanerd.deku.DefaultSMS.Commons;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.telephony.PhoneNumberUtils;
import android.text.SpannableString;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class HelpersTest {

    @Test
    public void testRegexForConversations() {
        String phoneNumber1 = "+237612345678";

        String website1 = "website.com";
        String website2 = "https://sub.site.website";
        String website3 = "sub.second.website/";
        String website4 = "sub.second.website/main.php";
        String website5 = "sub.second.website/main";
        String website6 = "sub.second.website/main/test/site.com";
        String website7 = "sub.second.website/main/test/site.com?page=0";
        String website8 = "https://github.com/simple-login/app/blob/master/docs/api.md#get-apialiasesalias_idcontacts";
        String website9 = "http://website.com";

        String email1 = "email@email.com";

        String testString = "Hello world! Now, let's encode " + website1 + " to communicate through " +
                "emails using " + email1 + " This should call " + phoneNumber1 +". Then another website " +
                "such as " + website2 + " and " + website3 + " and " + website4 + " and " +
                website5 + " and " + website6 + " and " + website7 + " and " + website8
                + " and to see about long queries. " + website9;

        String urlPattern = "((mailto:)?[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)" +
                "|((\\+?[0-9]{1,3}?)[ \\-]?)?([\\(]{1}[0-9]{3}[\\)])?[ \\-]?[0-9]{3}[ \\-]?[0-9]{4}" +
//                "|((https?|ftp|file)://)?[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                "|(https?://)?([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}(/[\\w\\.-]+)*(/\\S*)"+
                "|(https?://)?([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}(/[\\w\\.-]+)*";


        SpannableString spannableString = new SpannableString(testString);
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(spannableString);

        List<String> isPhoneCount = new ArrayList<>();
        List<String> isEmailCount = new ArrayList<>();
        List<String> isWebsiteCount = new ArrayList<>();

        List<String> isPhoneCountExpected = new ArrayList<>();
        isPhoneCountExpected.add(phoneNumber1);

        List<String> isEmailCountExpected = new ArrayList<>();
        isEmailCountExpected.add(email1);

        List<String> isWebsiteCountExpected = new ArrayList<>();
        isWebsiteCountExpected.add(website1);
        isWebsiteCountExpected.add(website2);
        isWebsiteCountExpected.add(website3);
        isWebsiteCountExpected.add(website4);
        isWebsiteCountExpected.add(website5);
        isWebsiteCountExpected.add(website6);
        isWebsiteCountExpected.add(website7);
        isWebsiteCountExpected.add(website8);
        isWebsiteCountExpected.add(website9);

        while (matcher.find()) {
            String tmp_url = matcher.group();
            if (PhoneNumberUtils.isWellFormedSmsAddress(tmp_url)) {
                isPhoneCount.add(tmp_url);
            } else if (tmp_url.contains("@")) {
                isEmailCount.add(tmp_url);
            } else {
                isWebsiteCount.add(tmp_url);
            }
        }

        Log.d(getClass().getName(), "Websites: " + isWebsiteCountExpected);
        Log.d(getClass().getName(), "Websites count: " + isWebsiteCount);

        assertArrayEquals(isEmailCountExpected.toArray(new String[0]),
                isEmailCount.toArray(new String[0]));

        assertArrayEquals(isWebsiteCountExpected.toArray(new String[0]),
                isWebsiteCount.toArray(new String[0]));

        assertArrayEquals(isPhoneCountExpected.toArray(new String[0]),
                isPhoneCount.toArray(new String[0]));
    }
}
