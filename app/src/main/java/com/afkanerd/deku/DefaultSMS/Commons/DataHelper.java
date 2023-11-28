package com.afkanerd.deku.DefaultSMS.Commons;

import java.nio.ByteBuffer;

public class DataHelper {

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

    public static String byteToBinary(byte[] bytes) {
        String binary = "";
        for(byte b: bytes)
            binary += Integer.toBinaryString(b);
        return binary;
    }

    public static int[] getNibbleFromByte(byte b) {
        return new int[]{(b & 0x0F), ((b >> 4) & 0x0F)};
    }

    public static String getHexOfByte(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            buffer.append(Character.forDigit((aByte >> 4) & 0xF, 16));
            buffer.append(Character.forDigit((aByte & 0xF), 16));
        }
        return buffer.toString();
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

    public static byte intToByte(int data) {
        return (byte) (data & 0xFF);
    }

    public static byte[] intArrayToByteArray(int[] intArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArray.length * 4); // 4 bytes per int
        for (int i = 0; i < intArray.length; i++) {
            byteBuffer.putInt(intArray[i]);
        }
        return byteBuffer.array();
    }


}
