package com.afkanerd.deku.DefaultSMS.Commons;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.i18n.phonenumbers.NumberParseException;

import org.junit.Test;

public class PhonenumberParsingTest {

    @Test
    public void checkForValidPhonenumber(){
        String wrongNumber = "https://example.com/02fkmb";
    }

    @Test
    public void formatPhoneNumbersNoPlus(){
        String nationalNumber = "612345678";
        String defaultRegion = "237";
        String phoneNumber = defaultRegion + nationalNumber;
        String formattedOutput = Helpers.getFormatNationalNumber(phoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersNoCountryCode(){
        String nationalNumber = "612345678";
        String defaultRegion = "237";
        String formattedOutput = Helpers.getFormatNationalNumber(nationalNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithCountryCode(){
        String fullPhoneNumber = "+237612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.getFormatNationalNumber(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_sms(){
        String fullPhoneNumber = "sms:+237612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.getFormatNationalNumber(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_smsto(){
        String fullPhoneNumber = "smsto:612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.getFormatNationalNumber(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_smsto_wrongCountryCode(){
        String fullPhoneNumber = "smsto:612345678";
        String defaultRegion = "1";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.getFormatNationalNumber(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersWithShared_smsto_wrongCountryCode1(){
        String fullPhoneNumber = "6505551212";
        String defaultRegion = "1";
        String nationalNumber = "6505551212";
        String formattedOutput = Helpers.getFormatNationalNumber(fullPhoneNumber, defaultRegion);
        assertEquals(nationalNumber, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersComplete(){
        String fullPhoneNumber = "smsto:+237612345678";
        String defaultRegion = "1";
        String nationalNumber = "612345678";
        String formattedOutput = Helpers.getFormatCompleteNumber(fullPhoneNumber, defaultRegion);
        assertEquals(("+" + "237" + nationalNumber), formattedOutput);
    }
    @Test
    public void formatPhoneNumbersComplete1(){
        String fullPhoneNumber = "smsto:6505551212";
        String defaultRegion = "0";
        String nationalNumber = "6505551212";
        String formattedOutput = Helpers.getFormatCompleteNumber(fullPhoneNumber, defaultRegion);
        assertEquals(("+" + defaultRegion + nationalNumber), formattedOutput);
    }

    @Test
    public void formatPhoneNumbersCompleteSharedContacts(){
        String fullPhoneNumber = "smsto:1%20(234)%20567-04";
        String defaultRegion = "1";
        String nationalNumber = "(234)567-04";
        String formattedOutput = Helpers.getFormatCompleteNumber(fullPhoneNumber, defaultRegion);
        assertEquals(("+" + defaultRegion + nationalNumber), formattedOutput);
    }

    @Test
    public void formatPhoneNumbersCompleteToStandards(){
        String fullPhoneNumber = "smsto:1%20(234)%20567-04";
        String defaultRegion = "1";
        String nationalNumber = "123456704";
        String formattedOutput = Helpers.getFormatForTransmission(fullPhoneNumber, defaultRegion);
        assertEquals(("+" + defaultRegion + nationalNumber), formattedOutput);
    }

    @Test
    public void formatPhoneNumbersAndCountryCodeTest() throws NumberParseException {
        String fullPhoneNumber = "+237612345678";
        String defaultRegion = "237";
        String nationalNumber = "612345678";
        String[] formattedOutput = Helpers.getCountryNationalAndCountryCode(fullPhoneNumber);
        assertArrayEquals(new String[]{defaultRegion, nationalNumber}, formattedOutput);
    }

    @Test
    public void formatPhoneNumbersAndCountryCodeSpecialCharacterTest() throws NumberParseException {
        String fullPhoneNumber = "123-456-789";
        String defaultRegion = "237";
        String nationalNumber = "123456789";
        String formattedOutput = Helpers.getFormatCompleteNumber(fullPhoneNumber, defaultRegion);
        assertEquals(("+" + defaultRegion + nationalNumber), formattedOutput);
    }

}
