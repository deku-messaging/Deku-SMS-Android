package com.afkanerd.deku.DefaultSMS.Commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import org.junit.Test;

public class HelpersTest {

    @Test
    public void formatPhoneNumbersNoPlus(){
        String nationalNumber = "612345678";
        String defaultRegion = "237";
        String phoneNumber = defaultRegion + nationalNumber;
        String formattedOutput = Helpers.formatPhoneNumbers(phoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersNoCountryCode(){
        String nationalNumber = "612345678";
        String defaultRegion = "237";
        String formattedOutput = Helpers.formatPhoneNumbers(nationalNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithCountryCode(){
        String fullPhoneNumber = "+237612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.formatPhoneNumbers(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_sms(){
        String fullPhoneNumber = "sms:+237612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.formatPhoneNumbers(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_smsto(){
        String fullPhoneNumber = "smsto:612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.formatPhoneNumbers(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }
}
