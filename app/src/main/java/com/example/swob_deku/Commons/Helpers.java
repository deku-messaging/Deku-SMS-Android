package com.example.swob_deku.Commons;

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
}
