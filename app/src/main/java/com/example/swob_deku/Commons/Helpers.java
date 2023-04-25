package com.example.swob_deku.Commons;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Set;

public class Helpers {
    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    public static long generateRandomNumber() {
        Random random = new Random();
        return random.nextInt(Integer.MAX_VALUE);
    }


    public static String[] convertSetToStringArray(Set<String> setOfString)
    {
        // Create String[] of size of setOfString
        String[] arrayOfString = new String[setOfString.size()];

        // Copy elements from set to string array
        // using advanced for loop
        int index = 0;
        for (String str : setOfString)
            arrayOfString[index++] = str;

        // return the formed String[]
        return arrayOfString;
    }

    public static String formatDate(Context context, long date) {
        // TODO: if yesterday - should show yesterday instead
        CharSequence formattedDate = new StringBuffer();

        if (DateUtils.isToday(date)) {
            formattedDate = DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
        }
        else {
            formattedDate = DateUtils.getRelativeDateTimeString(context, date,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_RELATIVE);
        }

        return formattedDate.toString();
    }

    public static String formatPhoneNumbers(String data) throws NumberParseException {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            String formattedData = data.replaceAll("%2B", "+")
                    .replaceAll("%20", "")
                    .replaceAll("-", "")
                    .replaceAll("\\s", "");

            Phonenumber.PhoneNumber parsedPhoneNumber = phoneNumberUtil.parse(formattedData, "US");
            // use the formattedPhoneNumber
            phoneNumberUtil.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            data = formattedData;
        } catch (NumberParseException e) {
            // handle the exception
            e.printStackTrace();
        }
        return data;
    }

    public static int generateColor(char letter) {
        int hue = (int) ((letter - 'A') * 15f) % 360; // Map letters to hue values
        float saturation = 0.7f; // Set fixed saturation and brightness values
        float brightness = 0.9f;
        float[] hsv = {hue, saturation, brightness};
        return Color.HSVToColor(hsv); // Convert HSB values to RGB color
    }
}
