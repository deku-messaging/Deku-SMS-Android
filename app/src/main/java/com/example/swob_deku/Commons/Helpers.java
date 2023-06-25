package com.example.swob_deku.Commons;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.Tag;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swob_deku.R;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // TODO: make this work for double sim phones
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

    public static boolean isWellFormedNumber(String data) {
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

    public static void highlightLinks(TextView textView, String text, int color) {
        // Regular expression to find URLs in the text
//        String urlPattern = "(https?://)?(www\\.)?[\\w\\d\\-]+(\\.[\\w\\d\\-]+)+([/?#]\\S*)?|(\\+\\d{1,3})\\d+";
//        String urlPattern = "(https?://)?(www\\.)?[\\w\\d\\-]+(\\.[\\w\\d\\-]+)+([/?#]\\S*)?|\\b\\+?\\d+\\b|\\b\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
//        String urlPattern = "(https?://)?(www\\.)?[\\w\\d\\-]+(\\.[\\w\\d\\-]+)+([/?#]\\S*)?|\\+\\b\\d+\\b|\\b\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
//        String urlPattern = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9\\-]+[.](?:[a-z]{2,}|xn\\-\\-[a-z0-9\\-]+))(?::\\d{2,5})?(?:/[\\S]*)?)";
//        String urlPattern = "(?i)(?:(?:https?://|www\\d{0,3}[.]|[a-z0-9\\-]+[.](?:[a-z]{2,}|xn\\-\\-[a-z0-9\\-]+))(?::\\d{2,5})?(?:/[\\S]*)?)|\\+\\d+|\\b\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*|\\b\\d{1,3}[-.\\s]\\d{1,3}[-.\\s]\\d{2,8}(?:[-.\\s]\\d{1,4})?";
        String urlPattern = "(?i)(?:(?:https?://|www\\d{0,3}[.]|[a-z0-9\\-]+[.](?:[a-z]{2,}|xn\\-\\-[a-z0-9\\-]+))(?::\\d{2,5})?(?:/[\\S]*)?)|\\+\\d+|\\b\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*|\\b\\d{1,3}[-.\\s]\\d{1,3}[-.\\s]\\d{2,8}(?:[-.\\s]\\d{1,4})?";

        SpannableString spannableString = new SpannableString(text);

        // Find all URLs in the text
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(spannableString);

        while (matcher.find()) {
            String tmp_url = matcher.group();
            if(PhoneNumberUtils.isWellFormedSmsAddress(tmp_url)) {
                final String tel = tmp_url;
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Uri phoneNumberUri = Uri.parse("tel:" + tel);
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, phoneNumberUri);
                        dialIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        widget.getContext().startActivity(dialIntent);
                    }
                };
                spannableString.setSpan(clickableSpan, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if(tmp_url.contains("@")) {
                final String email = tmp_url;
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse("mailto:" + email));
                        widget.getContext().startActivity(intent);
                    }
                };
                spannableString.setSpan(clickableSpan, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else {
                if (!tmp_url.startsWith("http://") && !tmp_url.startsWith("https://")) {
                    tmp_url = "http://" + tmp_url;
                }
                final String url = tmp_url;

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        widget.getContext().startActivity(intent);
                    }
                };
                spannableString.setSpan(clickableSpan, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            spannableString.setSpan(new ForegroundColorSpan(color),
                    matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spannableString);
    }
}
