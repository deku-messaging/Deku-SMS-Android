package com.example.swob_deku.Models.SMS;

import static java.time.Instant.now;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;


import androidx.annotation.NonNull;

import com.example.swob_deku.BroadcastSMSTextActivity;
import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.Commons.Helpers;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class SMSHandler {
    public static final int ASCII_MAGIC_NUMBER = 0x7F;
    public static final short DATA_TRANSMISSION_PORT = 8200;

    public static final Uri SMS_CONTENT_URI = Telephony.Sms.CONTENT_URI;

    public static final Uri SMS_INBOX_CONTENT_URI = Telephony.Sms.Inbox.CONTENT_URI;
    public static final Uri SMS_OUTBOX_CONTENT_URI = Telephony.Sms.Outbox.CONTENT_URI;
    public static final Uri SMS_SENT_CONTENT_URI = Telephony.Sms.Sent.CONTENT_URI;

    public static int countMessages(Context context, byte[] data) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        ArrayList<String> dividedMessage = smsManager.divideMessage(new String(data));
        return dividedMessage.size();
    }

    public static byte[] copyBytes(byte[] src, int startPos, int len) {
        byte[] dest = new byte[len];
        for(int i=startPos, j=0; i<src.length && j<len; ++i, j++)
            dest[j] = src[i];
        return dest;
    }

    public static ArrayList<byte[]> divideMessage(byte[] bytes) {
        final int FIRST_DIVIDE_CONST = 130;
        final int DIVIDE_CONST = 130;

        ArrayList<byte[]> messages = new ArrayList<>();

        if(bytes.length < DIVIDE_CONST)
            messages.add(bytes);
        else {
            messages.add(copyBytes(bytes, 0, FIRST_DIVIDE_CONST));
            for(int i=FIRST_DIVIDE_CONST;i<bytes.length; i+=DIVIDE_CONST) {
                messages.add(copyBytes(bytes, i, DIVIDE_CONST));
            }
        }

        return messages;
    }

    public static List<SMS> dateSegmentations(List<SMS> smsList) {
        List<SMS> copysmsList = new ArrayList<>(smsList);

        for(int i=smsList.size()-1;i>-1; --i) {
            SMS currentSMS = smsList.get(i);
            Date date = new Date(Long.parseLong(currentSMS.getDate()));
            Calendar currentCalendar = Calendar.getInstance();
            currentCalendar.setTime(date);

            if(i==smsList.size() -1 ) {
                copysmsList.add(new SMS(currentSMS.getDate()));
            }
            else {
                String previousDateString = smsList.get(i + 1).getDate();
                Date previousDate = new Date(Long.parseLong(previousDateString));
                Calendar prevCalendar = Calendar.getInstance();
                prevCalendar.setTime(previousDate);

                if ((prevCalendar.get(Calendar.HOUR_OF_DAY) != currentCalendar.get(Calendar.HOUR_OF_DAY)
                        || (prevCalendar.get(Calendar.DATE) != currentCalendar.get(Calendar.DATE)))) {
                    copysmsList.add(i+1, new SMS(currentSMS.getDate()));
                }
            }
        }

        return copysmsList;
    }

    public static void interpret_PDU(byte[] pdu) throws ParseException {

//                int[] receivedPDU = {0x07,0x91,0x32,0x67,0x49,0x00,0x00,0x71,0x24,0x0c,0x91,0x32,0x67,0x09,
//                        0x28,0x26,0x24,0x00,0x00,0x32,0x20,0x91,0x01,0x73,0x74,0x40,0x07,0xe8,0x72,
//                        0x1e,0xd4,0x2e,0xbb,0x01};
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU: " + pdu.length);

        String pduHex = DataHelper.getHexOfByte(pdu);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU: " + pduHex);

        int pduIterator = 0;

        byte SMSC_length = pdu[pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_length: " + (int) SMSC_length);

        byte SMSC_address_format = pdu[++pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_address_format: " +
                Integer.toHexString(SMSC_address_format));

        String SMSC_address_format_binary = DataHelper.byteToBinary(new byte[]{SMSC_address_format});
        parse_address_format(SMSC_address_format_binary.substring(SMSC_address_format_binary.length() - 7));

        byte[] SMSC_address = SMSHandler.copyBytes(pdu, ++pduIterator, --SMSC_length);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_address_format - binary: " + SMSC_address_format_binary);

        int[] addressHolder = DataHelper.bytesToNibbleArray(SMSC_address);
        String address = DataHelper.arrayToString(addressHolder);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_address: " + address);

        pduIterator += --SMSC_length;

        // TPDU begins
        byte first_octet = pdu[++pduIterator];
        String first_octet_binary = Integer.toBinaryString(first_octet);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU First octet binary: " + first_octet_binary);

        byte sender_address_length = pdu[++pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU Sender address length: " + (int)sender_address_length);

        byte sender_address_type = pdu[++pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU Sender address type: " +
                DataHelper.getHexOfByte(new byte[]{sender_address_type}));

        byte[] sender_address = copyBytes(pdu, ++pduIterator, sender_address_length / 2);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU Sender address: " +
                DataHelper.getHexOfByte(sender_address));

        addressHolder = DataHelper.bytesToNibbleArray(sender_address);
        address = DataHelper.arrayToString(addressHolder);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMS_Sender_address: " + address);

        pduIterator += (sender_address_length / 2)-1;

        byte PID = pdu[++pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU PID: " +
                DataHelper.getHexOfByte(new byte[]{PID}));

        byte DCS = pdu[++pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU DCS: " +
                DataHelper.getHexOfByte(new byte[]{DCS}));

        byte[] SCTS = copyBytes(pdu, ++pduIterator, 7);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SCTS: " +
                DataHelper.getHexOfByte(SCTS));
        String timestamp = DataHelper.arrayToString(DataHelper.bytesToNibbleArray(SCTS));
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SCTS: " + timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        Date date = sdf.parse(timestamp);

        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU Timestamp: " + date.toString());

        pduIterator += 7;

        byte UDL = pdu[pduIterator];
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU UDL: " +
                DataHelper.getHexOfByte(new byte[]{UDL}));

//        byte[] user_data = copyBytes(pdu, ++pduIterator, UDL);
//        String hex_user_data = DataHelper.getHexOfByte(user_data);
//        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU user data: " + hex_user_data);
//
//        String ascii_user_data = DataHelper.hexToAscii(hex_user_data);
//        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU user data ascii: " + ascii_user_data);

    }

    public static boolean hasUnreadMessages(Context context, String threadId) {
        try {
            Cursor cursor = context.getContentResolver().query(
                    SMS_INBOX_CONTENT_URI,
                    new String[] { Telephony.TextBasedSmsColumns.READ, Telephony.TextBasedSmsColumns.THREAD_ID },
//                    "read=? AND thread_id =? AND type != ?",
                    Telephony.TextBasedSmsColumns.READ + "=? AND " +
                            Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                    new String[] { "0",
                            String.valueOf(threadId) },
                    "date DESC LIMIT 1");

            boolean hasUnread = cursor.getCount() > 0;
            cursor.close();

            return hasUnread;
        }
        catch(Exception e ) {
            e.printStackTrace();
        }

        return false;
    }

    public static Cursor fetchSMSThreadIdFromAddress(@NonNull Context context, String address) {
        address = address.replaceAll("[\\s-]", "");

        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[] { Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID },
                "address like ?",
                new String[] { "%" + address},
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSMessagesAddress(@NonNull Context context, String address) {
        address = address.replaceAll("[\\s-]", "");

        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[] { Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE },
                "address like ?",
                new String[] { "%" + address},
                "date ASC");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSAddressFromThreadId(@NonNull Context context, String threadId) {
        String[] selection = new String[]{Telephony.TextBasedSmsColumns.ADDRESS};

        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                selection,
                Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                new String[]{threadId},
                null);

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSForThread(@NonNull Context context, String threadId) {
        String[] selection = new String[] { Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.PERSON,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.TYPE };

        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                selection,
                Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                new String[] { threadId },
                "date DESC");

        return smsMessagesCursor;
    }

    public static Cursor fetchSMSForThreading(Context context) {
        String[] projection = new String[] {
                Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.READ,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.TYPE,
                "MAX(date) as date"};

        return context.getContentResolver().query(
                SMS_CONTENT_URI,
                projection,
                "thread_id IS NOT NULL) GROUP BY (thread_id",
                null,
                "date DESC");
    }

    public static Cursor fetchSMSMessagesForSearch(Context context, String searchInput) {
        Uri targetedURI = Telephony.Sms.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[] { Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID, Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.PERSON, Telephony.TextBasedSmsColumns.DATE,Telephony.TextBasedSmsColumns.BODY, Telephony.TextBasedSmsColumns.TYPE },
                "body like '%" + searchInput + "%'",
                null,
                "date DESC");

        return cursor;
    }

    public static Cursor fetchSMSMessageForAllIds(Context context, ArrayList<Long> messageIds) {
        Uri targetedURI = Telephony.Sms.Inbox.CONTENT_URI;
        String selection = "_id=?";
        String[] selectionArgs = new String[messageIds.size()];
        selectionArgs[0] = String.valueOf(messageIds.get(0));

        for(int i=1;i<messageIds.size(); ++i) {
            selection += " OR _id=?";
            selectionArgs[i] = String.valueOf(messageIds.get(i));
        }

        Cursor cursor = context.getContentResolver().query(
                targetedURI,
                new String[] { Telephony.Sms._ID,
                        Telephony.TextBasedSmsColumns.STATUS,
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE },
                selection,
                selectionArgs,
                "date DESC");

        return cursor;
    }

    public static Cursor fetchSMSMessageThreadIdFromMessageId(Context context, long messageId) {
        Cursor cursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                 new String[] { Telephony.Sms._ID,
                         Telephony.TextBasedSmsColumns.THREAD_ID,
                         Telephony.TextBasedSmsColumns.ADDRESS,
                         Telephony.TextBasedSmsColumns.PERSON,
                         Telephony.TextBasedSmsColumns.DATE,
                         Telephony.TextBasedSmsColumns.BODY,
                         Telephony.TextBasedSmsColumns.TYPE },
                Telephony.Sms._ID + "=?",
                new String[] { String.valueOf(messageId)},
                "date DESC");

        return cursor;
    }

    public static long registerIncomingMessage(Context context, String address, String body) {
        long messageId = Helpers.generateRandomNumber();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

        try {
            context.getContentResolver().insert(SMS_INBOX_CONTENT_URI, contentValues);
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
        return messageId;
    }

    public static void registerFailedMessage(Context context, long messageId, int errorCode) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_FAILED);
        contentValues.put(Telephony.TextBasedSmsColumns.ERROR_CODE, errorCode);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED);

        try {
            context.getContentResolver().update(SMS_SENT_CONTENT_URI, contentValues, "_id=?",
                    new String[]{Long.toString(messageId)});
        } catch (Exception e ) {
            e.printStackTrace();
        }
    }

    public static void registerDeliveredMessage(@NonNull Context context, long messageId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_COMPLETE);

        try {
            context.getContentResolver().update(
                    SMS_SENT_CONTENT_URI,
                    contentValues,
                    Telephony.Sms._ID + "=?",
                    new String[]{Long.toString(messageId)});
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerSentMessage(Context context, long messageId) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);

        contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.STATUS_NONE);

        try {
            context.getContentResolver().update(
                    SMS_SENT_CONTENT_URI,
                    contentValues,
                    Telephony.Sms._ID + "=?",
                    new String[] {Long.toString(messageId)});
        } catch(Exception e ) {
            e.printStackTrace();
        }
    }

    public static String registerPendingMessage(Context context, String destinationAddress, String text, long messageId) {
        if(BuildConfig.DEBUG)
            Log.d(SMSHandler.class.getName(), "sending message id: " + messageId);

        String threadId = "";

        ContentValues contentValues = new ContentValues();

        contentValues.put(Telephony.Sms._ID, messageId);
        contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
        contentValues.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_PENDING);
        contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);
        contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

        try {

            Uri uri = context.getContentResolver().insert(
                    SMS_OUTBOX_CONTENT_URI,
                    contentValues );

            if(BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Outbox URI: " + uri.toString());

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] {Telephony.TextBasedSmsColumns.THREAD_ID},
                    null,
                    null,
                    null);

            if(cursor.moveToFirst()) {
                threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            }
        }
        catch(Exception e) {
//            e.printStackTrace();
            throw e;
        }

        return threadId;
    }

    public static void parse_address_format(String SMSC_address_format) {
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU parsing address format: " + SMSC_address_format);

        // TODO: compare and match the different TON and NPI values
        final String TON_INTERNATIONAL = "001";
        final String TON_NATIONAL = "010";

        final String NPI_ISDN = "0001";

        String SMSC_TON = SMSC_address_format.substring(0, 3);
        String SMSC_NPI = SMSC_address_format.substring(3);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_TON: " + SMSC_TON);
        Log.d(BroadcastSMSTextActivity.class.getName(), "PDU SMSC_NPI: " + SMSC_NPI);
    }

    public static void parse_first_octet(String SMS_first_octet) {
        // TODO: parse
    }

    public static void bad_experiments(Context context) {
        /**
         * The issue being faced is that they simply don't wanna let developers do this!
         * https://issuetracker.google.com/issues/36917186
         * https://pastebin.com/6uueFLCU
         * https://stackoverflow.com/questions/24464237/send-sms-in-pdu-mode-in-android
         */
        String DA = "+237690816242";
        PDUConverter.PDUEncoded pduEncoded = PDUConverter.encode("", DA, "", "hello_world");
        String encoded = pduEncoded.getPduEncoded();
//        Log.d(getLocalClassName(), "PDU encoded: " + encoded);

        SmsMessage smsMessage = SmsMessage.createFromPdu(DataHelper.hexStringToByteArray(encoded));
//        Log.d(getLocalClassName(), "PDU SMSC address: " + smsMessage.getMessageBody());

        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(null,
                DA,
                SMSHandler.DATA_TRANSMISSION_PORT, "hello world".getBytes(StandardCharsets.UTF_8),
                true);

//        submitPdu.encodedMessage = smsMessage.getPdu();

        Log.d(SMSHandler.class.getName(), "PDU message: " + DataHelper.toHexString(submitPdu.encodedMessage));

        /*
        * This shit is intended for rooted phones only
        // Get method "sendRawPdu"
        byte[] bb = new byte[1];
        SmsMessage.SubmitPdu mypdu = SmsMessage.getSubmitPdu(null, pNo, msg, true);
        size = (int) mypdu.encodedMessage[2];
        size = (size / 2) + (size % 2);

        mypdu.encodedMessage[size + 5] = (byte) 0xF0;

        Log.d(TAG, dumpHexString(mypdu.encodedMessage, 0, mypdu.encodedMessage.toString().length()));

        Method m2 = SmsManager.class.getDeclaredMethod("sendRawPdu", bb.getClass(),bb.getClass(),PendingIntent.class,PendingIntent.class,boolean.class,boolean.class);
        Log.d("success", "success getting sendRawPdu");

        m2.setAccessible(true);

        int length = msg.length();
        count = length / 160;
        int m = length % 160;
        if (m != 0) {
            count++;
        }

        m2.invoke(sm, mypdu.encodedScAddress,mypdu.encodedMessage,sentPI,deliveredPI, Boolean.valueOf(true),Boolean.valueOf(true));
        Log.d("success", "success sending message");
        */
    }

    public static String sendSMS(Context context, String destinationAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        String threadId = "";
        try {
            if(text.isEmpty() || destinationAddress.isEmpty())
                return "";

            try {
                threadId = registerPendingMessage(context, destinationAddress, text, messageId);
            } catch(Exception e ) {
                e.printStackTrace();
            }

            // TODO: Handle sending multipart messages
            ArrayList<String> dividedMessage = smsManager.divideMessage(text);
            if(dividedMessage.size() < 2 )
                smsManager.sendTextMessage(destinationAddress, null, text, sentIntent, deliveryIntent);
            else {
                ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();

                for(int i=0;i<dividedMessage.size() - 1; i++) {
                    sentPendingIntents.add(null);
                    deliveredPendingIntents.add(null);
                }

                sentPendingIntents.add(sentIntent);
                deliveredPendingIntents.add(deliveryIntent);

                smsManager.sendMultipartTextMessage(
                        destinationAddress,
                        null,
                        dividedMessage, sentPendingIntents, deliveredPendingIntents);
            }
        }
        catch(Throwable e) {
            // throw new IllegalArgumentException(e);
            throw e;
        }

        return threadId;
    }

    public static String sendSMS(Context context, String destinationAddress, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, long messageId) throws InterruptedException {
        SmsManager smsManager = Build.VERSION.SDK_INT > Build.VERSION_CODES.R ?
                context.getSystemService(SmsManager.class) : SmsManager.getDefault();

        if(data == null)
            return "";

        String threadId = "";

        String dataString = new String(data);
        try {
//            data = copyBytes(data, 0, 200);
            threadId = registerPendingMessage(context, destinationAddress, dataString, messageId);

            if(BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Sending data: " + new String(data));

//            dataString = "hello world";
//            ArrayList<String> dividedMessage = smsManager.divideMessage(dataString);
            ArrayList<byte[]> dividedMessage = divideMessage(data);

            // TODO: randomly generated number from 0 - 255

            // final byte sendingReferenceId = 0x00;

            final byte sendingReferenceId = (byte) (ASCII_MAGIC_NUMBER + new Random().nextInt(256));
            for(byte sendingMessageCounter = 0x00; sendingMessageCounter<dividedMessage.size(); ++sendingMessageCounter) {
                int dest = 0;
                byte[] rawData = dividedMessage.get(sendingMessageCounter);

                int totalSendingLength = sendingMessageCounter == 0x00 ? rawData.length + 3 :
                        rawData.length + 2;
                byte[] sendingData = new byte[totalSendingLength];
                sendingData[dest] = sendingReferenceId;
                sendingData[++dest] = sendingMessageCounter;

                // TODO: put this information before dividing it
                if(sendingMessageCounter == 0x00)
                    sendingData[++dest] = DataHelper.intToByte(dividedMessage.size());

                System.arraycopy(rawData, 0, sendingData, ++dest, rawData.length);

                PendingIntent sentIntentFinal = sendingMessageCounter == dividedMessage.size() -1 ?
                        sentIntent : null;

                PendingIntent deliveryIntentFinal = sendingMessageCounter == dividedMessage.size() -1 ?
                        deliveryIntent : null;

                smsManager.sendDataMessage(
                        destinationAddress,
                        null,
                        DATA_TRANSMISSION_PORT,
                        sendingData,
                        sentIntentFinal,
                        deliveryIntentFinal);

                if(BuildConfig.DEBUG)
                    Log.d(SMSHandler.class.getName(), "Sent counter: " + sendingMessageCounter);
                Thread.sleep(500);
            }
        } catch(Exception e ) {
            e.printStackTrace();
        }

        return threadId;
    }

    public static void updateThreadMessagesThread(Context context, String threadId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.TextBasedSmsColumns.READ, "1");
        try {
            int updateCount = context.getContentResolver().update(
                    SMS_CONTENT_URI,
                    contentValues,
                    Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND " + Telephony.TextBasedSmsColumns.READ +"=?",
                    new String[] { threadId, "0" });

            if(BuildConfig.DEBUG)
                Log.d(SMSHandler.class.getName(), "Updated read for: " + updateCount);
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
    }
}
