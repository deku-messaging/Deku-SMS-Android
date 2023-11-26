package com.afkanerd.deku.DefaultSMS.Commons;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

    public static Spannable highlightSubstringYellow(Context context, String text,
                                                     String searchString, boolean sent) {
        // Find all occurrences of the substring in the text.
        List<Integer> startIndices = new ArrayList<>();
        int index = text.toLowerCase().indexOf(searchString.toLowerCase());
        while (index >= 0) {
            startIndices.add(index);
            index = text.indexOf(searchString, index + searchString.length());
        }

        // Create a SpannableString object.
        SpannableString spannableString = new SpannableString(text);

        // Set the foreground color of the substring to yellow.
        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(
                context.getColor(sent ?
                        R.color.highlight_yellow_send :
                        R.color.highlight_yellow_received));
        for (int startIndex : startIndices) {
            spannableString.setSpan(backgroundColorSpan, startIndex, startIndex + searchString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }
    public static long generateRandomNumber() {
        Random random = new Random();
        return random.nextInt(Integer.MAX_VALUE);
    }

    public static int dpToPixel(float dpValue) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * density);
    }

    public static int getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        int color = r << 16 | g << 8 | b;

        return generateColor(color);
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

    public static boolean isShortCode(ThreadedConversations threadedConversations) {
        Pattern pattern = Pattern.compile("[a-zA-Z]");
        Matcher matcher = pattern.matcher(threadedConversations.getAddress());
        Log.d(Helpers.class.getName(), "Notifying for: " + threadedConversations.getAddress());
        return !PhoneNumberUtils.isWellFormedSmsAddress(threadedConversations.getAddress()) || matcher.find();
    }

    public static String getFormatCompleteNumber(String data, String defaultRegion) {
        data = data.replaceAll("%2B", "+")
                .replaceAll("%20", "");
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String outputNumber = data;
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(data, defaultRegion);
            long nationalNumber = phoneNumber.getNationalNumber();
            long countryCode = phoneNumber.getCountryCode();

            return "+" + countryCode + nationalNumber;
        } catch(NumberParseException e) {
            e.printStackTrace();
            if(e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                data = outputNumber.replaceAll("sms[to]*:", "");
                if (data.startsWith(defaultRegion)) {
                    outputNumber = "+" + data;
                } else {
                    outputNumber = "+" + defaultRegion + data;
                }
                return outputNumber;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static String[] getCountryNationalAndCountryCode(String data) throws NumberParseException {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(data, "");
        long nationNumber = phoneNumber.getNationalNumber();
        long countryCode = phoneNumber.getCountryCode();

        return new String[]{String.valueOf(countryCode), String.valueOf(nationNumber)};
    }

    public static String getFormatForTransmission(String data, String defaultRegion){
        data = data.replaceAll("%2B", "+")
                .replaceAll("%20", "")
                .replaceAll("sms[to]*:", "");

        // Remove any non-digit characters except the plus sign at the beginning of the string
        String strippedNumber = data.replaceAll("[^0-9+;]", "");
        if(strippedNumber.length() > 6) {
            // If the stripped number starts with a plus sign followed by one or more digits, return it as is
            if (!strippedNumber.matches("^\\+\\d+")) {
                strippedNumber = "+" + defaultRegion + strippedNumber;
            }
            return strippedNumber;
        }

        // If the stripped number is not a valid phone number, return an empty string
        return data;
    }

    public static String getFormatNationalNumber(String data, String defaultRegion) {
        data = data.replaceAll("%2B", "+")
                .replaceAll("%20", "");
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String outputNumber = data;
        try {
            try {
                Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(data, defaultRegion);

                return String.valueOf(phoneNumber.getNationalNumber());
            } catch(NumberParseException e) {
                if(e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                    data = data.replaceAll("sms[to]*:", "");
                    if (data.startsWith(defaultRegion)) {
                        outputNumber = "+" + data;
                    } else {
                        outputNumber = "+" + defaultRegion + data;
                    }
                    return getFormatNationalNumber(outputNumber, defaultRegion);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static String formatDateExtended(Context context, long epochTime) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - epochTime;

        Date currentDate = new Date(currentTime);
        Date targetDate = new Date(epochTime);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat fullDayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat shortDayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat shortMonthDayFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

//        if (diff < DateUtils.HOUR_IN_MILLIS) { // less than 1 hour
//            return DateUtils.getRelativeTimeSpanString(epochTime, currentTime, DateUtils.MINUTE_IN_MILLIS).toString();
//        }
        if (diff < DateUtils.DAY_IN_MILLIS) { // less than 1 day
            return DateUtils.formatDateTime(context, epochTime, DateUtils.FORMAT_SHOW_TIME);
        } else if (isSameDay(currentDate, targetDate)) { // today
            return timeFormat.format(targetDate);
        } else if (isYesterday(currentDate, targetDate)) { // yesterday
            return context.getString(R.string.single_message_thread_yesterday) + " • " + timeFormat.format(targetDate);
        } else if (isSameWeek(currentDate, targetDate)) { // within the same week
            return fullDayFormat.format(targetDate) + " • " + timeFormat.format(targetDate);
        } else { // greater than 1 week
            return shortDayFormat.format(targetDate) + ", " + shortMonthDayFormat.format(targetDate)
                    + " • " + timeFormat.format(targetDate);
        }
    }

    private static boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyDDD", Locale.getDefault());
        String day1 = dayFormat.format(date1);
        String day2 = dayFormat.format(date2);
        return day1.equals(day2);
    }

    private static boolean isYesterday(Date date1, Date date2) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyDDD", Locale.getDefault());
        String day1 = dayFormat.format(date1);
        String day2 = dayFormat.format(date2);

        int dayOfYear1 = Integer.parseInt(day1.substring(4));
        int dayOfYear2 = Integer.parseInt(day2.substring(4));
        int year1 = Integer.parseInt(day1.substring(0, 4));
        int year2 = Integer.parseInt(day2.substring(0, 4));

        return (year1 == year2 && dayOfYear1 - dayOfYear2 == 1)
                || (year1 - year2 == 1 && dayOfYear1 == 1 && dayOfYear2 == 365);
    }

    private static boolean isSameWeek(Date date1, Date date2) {
        SimpleDateFormat weekFormat = new SimpleDateFormat("yyyyww", Locale.getDefault());
        String week1 = weekFormat.format(date1);
        String week2 = weekFormat.format(date2);
        return week1.equals(week2);
    }

    public static String formatDate(Context context, long epochTime) {
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
        return String.valueOf(PhoneNumberUtil.getInstance().getCountryCodeForRegion(countryCode));
    }

    public static int generateColor(int input) {
        int hue;
        int saturation = 100;
        int value = 60; // Reduced value component for darker colors

        hue = Math.abs(input * 31 % 360);
        // Convert the HSV color to RGB and return the color as an int
        float[] hsv = {hue, saturation, value};
        int color = Color.HSVToColor(hsv);
        return color;
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
        if(text == null)
            return;
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
            if (PhoneNumberUtils.isWellFormedSmsAddress(tmp_url)) {
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
            } else if (tmp_url.contains("@")) {
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
            } else {
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


    public static boolean isSameMinute(Long date1, Long date2) {
        java.util.Date date = new java.util.Date(date1);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(date);

        String previousDateString = String.valueOf(date2);
        java.util.Date previousDate = new java.util.Date(Long.parseLong(previousDateString));
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.setTime(previousDate);

        return !((prevCalendar.get(Calendar.HOUR_OF_DAY) != currentCalendar.get(Calendar.HOUR_OF_DAY)
                || (prevCalendar.get(Calendar.MINUTE) != currentCalendar.get(Calendar.MINUTE))
                || (prevCalendar.get(Calendar.DATE) != currentCalendar.get(Calendar.DATE))));
    }

    public static boolean isSameHour(Long date1, Long date2) {
        java.util.Date date = new java.util.Date(date1);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(date);

        String previousDateString = String.valueOf(date2);
        java.util.Date previousDate = new java.util.Date(Long.parseLong(previousDateString));
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.setTime(previousDate);

        return !((prevCalendar.get(Calendar.HOUR_OF_DAY) != currentCalendar.get(Calendar.HOUR_OF_DAY)
                || (prevCalendar.get(Calendar.DATE) != currentCalendar.get(Calendar.DATE))));
    }

}
