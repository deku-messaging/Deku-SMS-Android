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

    public static String getHexOfByte(byte[] b) {
        String hexString = "";
        for(byte b1 : b)
            hexString += Integer.toHexString(b1 & 0xFF).toUpperCase();
        return hexString;
    }

    public static byte[] intArrayToByteArray(int[] ints) {
        byte[] array = new byte[ints.length];

        for(int i=0;i<ints.length;++i)
            array[i] = (byte) ints[i];

        return array;
    }

    public static int[] nibbleToIntArray(byte[] bytes) {
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
}
