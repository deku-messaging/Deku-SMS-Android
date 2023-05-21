package com.example.swob_deku.Commons;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.nfc.Tag;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;

public class Helpers {
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

    public static String formatDate(Context context, long epochTime) {
//        // TODO: if yesterday - should show yesterday instead
//        CharSequence formattedDate = new StringBuffer();
//
//        if (DateUtils.isToday(date)) {
//            formattedDate = DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
//                    DateUtils.MINUTE_IN_MILLIS);
//        }
//        else {
//            formattedDate = DateUtils.getRelativeDateTimeString(context, date,
//                    DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS,
//                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_RELATIVE);
//        }
//
//        return formattedDate.toString();
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - epochTime;

        if (diff < DateUtils.HOUR_IN_MILLIS) { // less than 1 hour
            return DateUtils.getRelativeTimeSpanString(epochTime, currentTime, DateUtils.MINUTE_IN_MILLIS).toString();
        } else if (diff < DateUtils.DAY_IN_MILLIS) { // less than 1 day
            return DateUtils.formatDateTime(context, epochTime, DateUtils.FORMAT_SHOW_TIME);
        } else if (diff < DateUtils.WEEK_IN_MILLIS) { // less than 1 week
            return DateUtils.formatDateTime(context, epochTime, DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY);
        } else { // greater than 1 week
            return DateUtils.formatDateTime(context, epochTime, DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE);
        }
    }

    public static String getUserCountry(Context context) {
        String countryCode = null;

        // Check if network information is available
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            // Get the TelephonyManager to access network-related information
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                // Get the ISO country code from the network
                countryCode = tm.getNetworkCountryIso().toUpperCase(Locale.US);
            }
        }
        return String.valueOf(
                PhoneNumberUtil.createInstance(context).getCountryCodeForRegion(countryCode));
//        return countryCode;
    }

    public static String formatPhoneNumbers(Context context, String data) throws NumberParseException {
        String formattedString = data.replaceAll("%2B", "+")
                .replaceAll("%20", "")
                .replaceAll("-", "")
                .replaceAll("\\s", "");

        // Remove any non-digit characters except the plus sign at the beginning of the string
        String strippedNumber = formattedString.replaceAll("[^0-9+]", "");

        // If the stripped number starts with a plus sign followed by one or more digits, return it as is
        if (strippedNumber.matches("^\\+\\d+") )
            return strippedNumber;
        else if(strippedNumber.length() >=7) {
            String dialingCode = getUserCountry(context);
           strippedNumber = "+" + dialingCode + strippedNumber;
           return strippedNumber;
        }

        // If the stripped number is not a valid phone number, return an empty string
        return data;
    }

    public static int generateColor(String input) {
        int hue;
        int saturation = 100;
        int value = 60; // Reduced value component for darker colors

        if (input.length() == 0) {
            // Return a default color if the input is empty
            hue = 0;
        } else if (input.length() == 1) {
            // Use the first character of the input to generate the hue
            char firstChar = input.charAt(0);
            hue = Math.abs(firstChar * 31 % 360);
        } else {
            // Use the first and second characters of the input to generate the hue
            char firstChar = input.charAt(0);
            char secondChar = input.charAt(1);
            hue = Math.abs((firstChar + secondChar) * 31 % 360);
        }

        // Convert the HSV color to RGB and return the color as an int
        float[] hsv = {hue, saturation, value};
        int color = Color.HSVToColor(hsv);
        return color;
    }

    public static boolean isBase64Encoded(String input) {
        try {
            byte[] decodedBytes = Base64.decode(input, Base64.DEFAULT);
//            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

//            Log.d(Helpers.class.getName(), "De-Encoded string: " + decodedString);

            String reencodedString = Base64.encodeToString(decodedBytes, Base64.DEFAULT)
                            .replaceAll("\\n", "");

            Log.d(Helpers.class.getName(), "Re-Encoded string: " + reencodedString);

            return input.replaceAll("\\n", "").equals(reencodedString);
        } catch (Exception e) {
            return false;
        }
    }
}
