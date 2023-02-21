package com.example.swob_deku.Commons;

import android.util.Log;

import java.io.ByteArrayOutputStream;

public class DataHelper {

    public static int[] getNibbleFromByte(byte b) {
        return new int[]{(b & 0x0F), ((b >> 4) & 0x0F)};
    }

    public static String byteToBinary(byte[] bytes) {
        String binary = "";
        for(byte b: bytes)
            binary += Integer.toBinaryString(b);
        return binary;
    }

    public static char[] byteToChar(byte[] bytes) {
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        return chars;
    }

    public static String arrayToString(int[] intArray) {
        StringBuilder stringBuilder = new StringBuilder();
        if (intArray.length > 0) {
            stringBuilder.append(intArray[0]);
            for (int i = 1; i < intArray.length; i++) {
                stringBuilder.append(intArray[i]);
            }
        }
        return stringBuilder.toString();
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();


        for(byte b: bytes)
            stringBuilder.append(Integer.toHexString(b));
        return stringBuilder.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    public static String getHexOfByte(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            buffer.append(Character.forDigit((aByte >> 4) & 0xF, 16));
            buffer.append(Character.forDigit((aByte & 0xF), 16));
        }
        return buffer.toString();
    }

    public static byte[] intArrayToByteArray(int[] ints) {
        byte[] array = new byte[ints.length];

        for(int i=0;i<ints.length;++i)
            array[i] = (byte) ints[i];

        return array;
    }

    public static byte[] stringToNibble(String strData) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for(int i=0; i<strData.length(); i+=2 ) {
            int val1 = Integer.parseInt(strData.substring(i, i+1));
            int val2 = Integer.parseInt(strData.substring(i+1, i+2));
            byteArrayOutputStream.write((byte) ((val1 << 4) | val2 ));
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static int[] bytesToNibbleArray(byte[] bytes) {
        int ints[] = new int[bytes.length * 2];
        for(int i=0, j=0;i<bytes.length; ++i, j+=2) {
            int[] data = getNibbleFromByte(bytes[i]);
            ints[j] = data[0];
            ints[j+1] = data[1];
        }
        return ints;
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    private static String destinationNumberLength(String number) {
        number = number.replace("[^\\d]", "");
        return stringHexLength(number.length());
    }

    /**
     * returns the length of given length l as hexvalue String.
     *
     * @param l
     * @return
     */
    private static String stringHexLength(int l) {
        String length = Integer.toHexString(l);
        if (length.length() < 2)
            length = "0" + length;

        return length;
    }

}
