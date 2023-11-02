package com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB;

import static com.afkanerd.deku.DefaultSMS.Commons.Helpers.getUserCountry;
import static com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler.SMS_CONTENT_URI;
import static com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler.SMS_INBOX_CONTENT_URI;
import static com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB.SMSHandler.SMS_OUTBOX_CONTENT_URI;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.telephony.PhoneNumberUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSMetaEntity {
    public static final String THREAD_ID = "THREAD_ID";
    public static final String ADDRESS = "ADDRESS";
    public static final String ID = "ID";
    public static final String TYPE_DATA_KEY = "TYPE_DATA_KEY";

    public static final String SHARED_SMS_BODY = "sms_body";

    public enum ENCRYPTION_STATE {
        NOT_ENCRYPTED,
        SENT_PENDING_AGREEMENT,
        RECEIVED_PENDING_AGREEMENT,
        RECEIVED_AGREEMENT_REQUEST,
        ENCRYPTED
    }

    private String address, threadId;
    private String _address;

    private long newestDateTime;
    private int newestType;
    private boolean newestIsRead = false;

    private boolean isContact = false;

    private String contactName;

    private String formattedDate;

    private String messageId;

    public long getNewestDateTime() {
        return this.newestDateTime;
    }

    public void setThreadId(Context context, String threadId) {
        this.threadId = threadId;
        Cursor cursor = fetchMessages(context, 1, 0);
        if(cursor.moveToFirst()) {
            int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
            int dateTimeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
            int typeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE);
            int readIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.READ);

            this.address = cursor.getString(addressIndex);
            this.newestDateTime = Long.parseLong(cursor.getString(dateTimeIndex));
            this.newestType = cursor.getInt(typeIndex);
            this.newestIsRead = cursor.getInt(readIndex) != 0;
            cursor.close();
            this.isContact = getIsContact(context);
            if(this.isContact) {
                this.contactName = Contacts.retrieveContactName(context, address);
            }
            this.formattedDate = Helpers.formatDate(context, this.newestDateTime);
        }

        try {
            this._address = formatPhoneNumbers(context, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFormattedDate() {
        return this.formattedDate;
    }

    public String getContactName() {
        return this.contactName;
    }

    public boolean isContact() {
        return this.isContact;
    }

    private boolean getIsContact(Context context) {
        String addressInPhone = Contacts.retrieveContactName(context, this.address);
        return !addressInPhone.isEmpty() && !addressInPhone.equals("null");
    }

    public boolean getNewestIsRead() {
        return this.newestIsRead;
    }

    public int getNewestType() {
        return this.newestType;
    }

    public String setAddress(Context context, String address) {
        this.address = address;
        try {
            this._address = formatPhoneNumbers(context, this.address);
        } catch (Exception e) {
            e.printStackTrace();
            this._address = this.address;
        }

        return _findSMSThreadIdFromAddress(context, this._address);
    }

    public static String _findSMSThreadIdFromAddress(Context context, String address) {
        String selection = "address = ?";
        String[] selectionArgs = { address };

        Cursor cursor = context.getContentResolver().query(
                SMS_CONTENT_URI,
                new String[]{"thread_id"},
                selection,
                selectionArgs,
                "date DESC LIMIT 1");

        String threadId = "";
        if (cursor.moveToFirst()) {
            int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
            threadId = cursor.getString(threadIdIndex);

            cursor.close();
            return threadId;
        }
        return null;
    }

    public String getThreadId() {
        return this.threadId;
    }
    public String getAddress(){
        return this._address;
    }

    /**
     * checks if number contains letters and if matches android default wellformedaddresses
     * @return boolean - true if short code false otherwise
     */
    public boolean isShortCode() {
        Pattern pattern = Pattern.compile("[a-zA-Z]");
        Matcher matcher = pattern.matcher(getAddress());
        return !PhoneNumberUtils.isWellFormedSmsAddress(getAddress()) || matcher.find();
    }

    private String formatPhoneNumbers(Context context, String data) throws NumberParseException {
        String formattedString = data.replaceAll("%2B", "+")
                .replaceAll("%20", "");

        if(!PhoneNumberUtils.isWellFormedSmsAddress(formattedString))
            return formattedString;

        // Remove any non-digit characters except the plus sign at the beginning of the string
        String strippedNumber = formattedString.replaceAll("[^0-9+;]", "");
        if(strippedNumber.length() > 6) {
            // If the stripped number starts with a plus sign followed by one or more digits, return it as is
            if (!strippedNumber.matches("^\\+\\d+")) {
                String dialingCode = getUserCountry(context);
                strippedNumber = "+" + dialingCode + strippedNumber;
            }
            return strippedNumber;
        }

        // If the stripped number is not a valid phone number, return an empty string
        return data;
    }

    public String getContactName(Context context) {
        try {
            String contactName = Contacts.retrieveContactName(context, getAddress());
            if(!contactName.isEmpty())
                return contactName;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return this._address;
    }

    /**
     *
     * @param context
     * @return ENCRYPTION_STATE: Informs about the encryption with current address that holds
     * this entity. Remember, it is always the address' state with you - not yours!
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public ENCRYPTION_STATE getEncryptionState(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        if (securityECDH.hasSecretKey(getAddress())) {
            return ENCRYPTION_STATE.ENCRYPTED;
        }

        if(securityECDH.peerAgreementPublicKeysAvailable(context, this.getAddress()) &&
                securityECDH.hasPrivateKey(getAddress())) {
            return ENCRYPTION_STATE.RECEIVED_PENDING_AGREEMENT;
        }

        if (securityECDH.peerAgreementPublicKeysAvailable(context, this.getAddress())) {
            return ENCRYPTION_STATE.RECEIVED_AGREEMENT_REQUEST;
        }

        if(securityECDH.hasPrivateKey(getAddress())) {
            return ENCRYPTION_STATE.SENT_PENDING_AGREEMENT;
        }

        return ENCRYPTION_STATE.NOT_ENCRYPTED;
    }

    /**
     * First time this is used is the peer generating the initiating agreement key.
     * Second time this is used is the peer generating the follow up agreement key.
     *
     * @param context
     * @return byte[] : Returns the public key. Remember this is your
     * primary key (you being whomever is initiating the handshake).
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public KeyPair generateAgreements(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        securityECDH.removeAllKeys(getAddress());
        return securityECDH.generateKeyPair();
    }

    /**
     *
     * @param context
     * @return byte[] : Returns the public key generated from the peer agreement key.
     * Remember this is your primary key (you being whomever is initiating the handshake).
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public byte[] agreePeerRequest(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        String peerPublicKeyEncoded = securityECDH.getPeerAgreementPublicKey(getAddress());

        byte[] peerPublicKey = Base64.decode(peerPublicKeyEncoded, Base64.DEFAULT);

        KeyPair keyPair = securityECDH.generateKeyPairFromPublicKey(peerPublicKey);
        PrivateKey privateKey = securityECDH.hasPrivateKey(getAddress()) ?
                securityECDH.securelyFetchPrivateKey(getAddress()) : keyPair.getPrivate();

        byte[] secret = securityECDH.generateSecretKey(peerPublicKey, privateKey);
        securityECDH.securelyStoreSecretKey(getAddress(), secret);

        return SecurityHelpers.txAgreementFormatter(keyPair.getPublic().getEncoded());
    }

    public byte[] getSecretKey(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        return Base64.decode(securityECDH.securelyFetchSecretKey(getAddress()), Base64.DEFAULT);
    }

    public boolean hasSecretKey(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        return securityECDH.hasSecretKey(getAddress());
    }

    public String encryptContent(Context context, String data) throws Throwable {
        byte[] encryptedContent = SecurityAES.encrypt_256_cbc(data.getBytes(StandardCharsets.UTF_8),
                getSecretKey(context), null);
        return Base64.encodeToString(encryptedContent, Base64.DEFAULT);
    }

    public void call(Context context) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.setData(Uri.parse("tel:" + getAddress()));

        context.startActivity(callIntent);
    }

    public boolean isEncrypted(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        return securityECDH.hasSecretKey(getAddress());
    }

    public boolean isPendingAgreement(Context context) throws GeneralSecurityException, IOException {
        SecurityECDH securityECDH = new SecurityECDH(context);
        return securityECDH.peerAgreementPublicKeysAvailable(context, getAddress());
    }

    public void deleteMessage(Context context, String messageId) throws Exception {
        try {
            int updateCount = context.getContentResolver().delete(
                    SMS_CONTENT_URI,
                    Telephony.Sms._ID + "=?",
                    new String[]{messageId});

        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void deleteMultipleMessages(Context context, String[] ids) {
        try {
            int updateCount = context.getContentResolver().delete(SMS_CONTENT_URI,
                    Telephony.Sms._ID + " in (" +
                            TextUtils.join(",", Collections.nCopies(ids.length, "?")) + ")", ids);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasUnreadMessages(Context context) {
        try {
            Cursor cursor = context.getContentResolver().query(
                    SMS_INBOX_CONTENT_URI,
                    new String[]{Telephony.TextBasedSmsColumns.READ, Telephony.TextBasedSmsColumns.THREAD_ID},
//                    "read=? AND thread_id =? AND type != ?",
                    Telephony.TextBasedSmsColumns.READ + "=? AND " +
                            Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                    new String[]{"0",
                            String.valueOf(threadId)},
                    "date DESC LIMIT 1");

            boolean hasUnread = cursor.getCount() > 0;
            cursor.close();

            return hasUnread;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Cursor fetchMessages(@NonNull Context context, int limit, int offset) {
        String constrains = "date DESC" + (limit > 0 ? " LIMIT " + limit : "") +
                (offset > 0 ? " OFFSET " + offset : "");

        String[] selection = new String[]{Telephony.Sms._ID,
                Telephony.TextBasedSmsColumns.STATUS,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.PERSON,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.READ,
                Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID,
                Telephony.TextBasedSmsColumns.TYPE};

        return context.getContentResolver().query(
                SMS_CONTENT_URI,
                selection,
                Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                new String[]{threadId},
                constrains);
    }

    public Cursor fetchUnreadMessages(Context context) {
        return context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[]{Telephony.Sms._ID,
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.TYPE},
                Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND "
                        + Telephony.Sms.READ + "=?",
                new String[]{threadId, "0"},
                "date ASC");
    }

    public Cursor fetchOutboxMessage(@NonNull Context context, long messageId) {
        Cursor smsMessagesCursor = context.getContentResolver().query(
                SMS_OUTBOX_CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.PERSON,
                        Telephony.TextBasedSmsColumns.DATE,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID,
                        Telephony.TextBasedSmsColumns.TYPE},
                Telephony.TextBasedSmsColumns.THREAD_ID + "=? AND " + Telephony.Sms._ID + "=?",
                new String[]{threadId, String.valueOf(messageId)},
                null);

        return smsMessagesCursor;
    }
}

