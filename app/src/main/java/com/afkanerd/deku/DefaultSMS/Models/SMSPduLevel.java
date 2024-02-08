package com.afkanerd.deku.DefaultSMS.Models;

import android.util.Log;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;

import java.text.ParseException;

public class SMSPduLevel {

    public static void interpret_PDU(byte[] pdu) throws ParseException {
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU: " + pdu.length);
//
//        String pduHex = DataHelper.getHexOfByte(pdu);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU: " + pduHex);
//
//        int pduIterator = 0;
//
//        byte SMSC_length = pdu[pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_length: " + (int) SMSC_length);
//
//        byte SMSC_address_format = pdu[++pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_address_format: " +
//                Integer.toHexString(SMSC_address_format));
//
//        String SMSC_address_format_binary = DataHelper.byteToBinary(new byte[]{SMSC_address_format});
//        parse_address_format(SMSC_address_format_binary.substring(SMSC_address_format_binary.length() - 7));
//
//        byte[] SMSC_address = SMSHandler.copyBytes(pdu, ++pduIterator, --SMSC_length);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_address_format - binary: " + SMSC_address_format_binary);
//
//        int[] addressHolder = DataHelper.bytesToNibbleArray(SMSC_address);
//        String address = DataHelper.arrayToString(addressHolder);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_address: " + address);
//
//        pduIterator += --SMSC_length;
//
//        // TPDU begins
//        byte first_octet = pdu[++pduIterator];
//        String first_octet_binary = Integer.toBinaryString(first_octet);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU First octet binary: " + first_octet_binary);
//
//        byte sender_address_length = pdu[++pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU Sender address length: " + (int) sender_address_length);
//
//        byte sender_address_type = pdu[++pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU Sender address type: " +
//                DataHelper.getHexOfByte(new byte[]{sender_address_type}));
//
//        byte[] sender_address = copyBytes(pdu, ++pduIterator, sender_address_length / 2);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU Sender address: " +
//                DataHelper.getHexOfByte(sender_address));
//
//        addressHolder = DataHelper.bytesToNibbleArray(sender_address);
//        address = DataHelper.arrayToString(addressHolder);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMS_Sender_address: " + address);
//
//        pduIterator += (sender_address_length / 2) - 1;
//
//        byte PID = pdu[++pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU PID: " +
//                DataHelper.getHexOfByte(new byte[]{PID}));
//
//        byte DCS = pdu[++pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU DCS: " +
//                DataHelper.getHexOfByte(new byte[]{DCS}));
//
//        byte[] SCTS = copyBytes(pdu, ++pduIterator, 7);
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SCTS: " +
//                DataHelper.getHexOfByte(SCTS));
//        String timestamp = DataHelper.arrayToString(DataHelper.bytesToNibbleArray(SCTS));
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SCTS: " + timestamp);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
//        Date date = sdf.parse(timestamp);
//
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU Timestamp: " + date.toString());
//
//        pduIterator += 7;
//
//        byte UDL = pdu[pduIterator];
//        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU UDL: " +
//                DataHelper.getHexOfByte(new byte[]{UDL}));
    }

    public static void parse_address_format(String SMSC_address_format) {
        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU parsing address format: " + SMSC_address_format);

        // TODO: compare and match the different TON and NPI values
        final String TON_INTERNATIONAL = "001";
        final String TON_NATIONAL = "010";

        final String NPI_ISDN = "0001";

        String SMSC_TON = SMSC_address_format.substring(0, 3);
        String SMSC_NPI = SMSC_address_format.substring(3);
        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_TON: " + SMSC_TON);
        Log.d(IncomingTextSMSBroadcastReceiver.class.getName(), "PDU SMSC_NPI: " + SMSC_NPI);
    }

    // A function that takes a pdu string as input and returns an array of two strings: the OA and DA
    public static String[] extractOAandDA(byte[] pduBytes) {
        // First, we need to convert the pdu string into a byte array using the hexStringToByteArray method
//        byte[] pduBytes = hexStringToByteArray(pdu);

        // Next, we need to get the length of the OA and DA from the first and third byte of the pdu
        int oaLength = pduBytes[1] & 0xFF; // mask the sign bit
        int daLength = pduBytes[3] & 0xFF; // mask the sign bit

        // Then, we need to get the OA and DA from the pdu byte array using the decodePhoneNumber method
        String oa = decodePhoneNumber(pduBytes, 2, oaLength); // start from the second byte
        String da = decodePhoneNumber(pduBytes, 4, daLength); // start from the fourth byte

        // Finally, we return an array of two strings containing the OA and DA
        String[] result = {oa, da};
        return result;
    }

    // A helper method that converts a hex string into a byte array
    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    // A helper method that decodes a phone number from a pdu byte array
    public static String decodePhoneNumber(byte[] pdu, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            // Swap the two nibbles of each byte
            byte b = pdu[i];
            int high = (b & 0xF0) >> 4;
            int low = (b & 0x0F);
            b = (byte) ((low << 4) | high);

            // Convert the byte to a hex string
            String hex = Integer.toHexString(b & 0xFF);

            // Append the hex string to the phone number, ignoring the trailing F
            if (hex.charAt(0) != 'f') {
                sb.append(hex.charAt(0));
            }
            if (hex.charAt(1) != 'f') {
                sb.append(hex.charAt(1));
            }
        }
        return sb.toString();
    }


}
