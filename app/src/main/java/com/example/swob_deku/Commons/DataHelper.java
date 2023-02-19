package com.example.swob_deku.Commons;

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


    public static String getHexOfByte(byte[] b) {
        String hexString = "";
        for(byte b1 : b)
            hexString += Integer.toHexString(b1 & 0xFF).toUpperCase() + " ";
        return hexString;
    }
}
