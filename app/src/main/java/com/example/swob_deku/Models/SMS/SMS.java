package com.example.swob_deku.Models.SMS;

import static com.example.swob_deku.Commons.Helpers.getUserCountry;
import static com.example.swob_deku.Models.SMS.SMSHandler.SMS_CONTENT_URI;
import static com.example.swob_deku.Models.SMS.SMSHandler.SMS_INBOX_CONTENT_URI;
import static com.example.swob_deku.Models.SMS.SMSHandler.SMS_OUTBOX_CONTENT_URI;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMS {
    // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#constants_1

    public String body;
    public String address;
    public String threadId;
    public String date;
    int type;
    public String errorCode;
    public int statusCode;
    public String id;
    public int read;
    public String routerStatus;
    public int subscriptionId;
    public String displayName;
    public ArrayList<String> routingUrls = new ArrayList<>();

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }


    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int isRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getRead() {
        return read;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public ArrayList<String> getRoutingUrls() {
        return routingUrls;
    }

    public String getRouterStatus() {
        return this.routerStatus;
    }

    public void setRouterStatus(String routerStatus) {
        this.routerStatus = routerStatus;
    }


    public void setRoutingUrls(ArrayList<String> routingUrls) {
        this.routingUrls = routingUrls;
    }

    public void addRoutingUrl(String routingUrl) {
        this.routingUrls.add(routingUrl);
    }

    // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns
    public SMS(Cursor cursor) {
        int bodyIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY);
        int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
        int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
        int dateIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
        int typeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE);
        int errorCodeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ERROR_CODE);
        int statusCodeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.STATUS);
        int readIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.READ);
        int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
        int subscriptionIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID);

        this.type =  cursor.getInt(typeIndex);
        this.body = String.valueOf(cursor.getString(bodyIndex));
        this.address = String.valueOf(cursor.getString(addressIndex));
        this.threadId = cursor.getString(threadIdIndex);
        this.date = String.valueOf(cursor.getString(dateIndex));

        if(subscriptionIdIndex > -1)
            this.subscriptionId = cursor.getInt(subscriptionIdIndex);

        if(idIndex > -1 ) {
            this.id = String.valueOf(cursor.getString(idIndex));
        }

        if(readIndex > -1 ) {
            this.read = cursor.getInt(readIndex);
        }

        if(threadIdIndex > -1 )
            this.threadId = String.valueOf(cursor.getString(threadIdIndex));

        if(errorCodeIndex > -1 )
            this.errorCode = String.valueOf(cursor.getString(errorCodeIndex));
        if(statusCodeIndex > -1 )
            this.statusCode = cursor.getInt(statusCodeIndex);

    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof SMS) {
            SMS sms = (SMS) obj;

            return sms.getId().equals(this.id) &&
                    sms.threadId.equals(this.threadId) &&
                    sms.address.equals(this.address) &&
                    sms.body.equals(this.body) &&
                    sms.statusCode == this.statusCode &&
                    sms.read == this.read &&
                    sms.date.equals(this.date);
        }
        return false;
    }

    public static final DiffUtil.ItemCallback<SMS> DIFF_CALLBACK = new DiffUtil.ItemCallback<SMS>() {
        @Override
        public boolean areItemsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
            return oldItem.equals(newItem);
        }
    };

    public static class SMSMetaEntity {
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

        public void setThreadId(Context context, String threadId) {
            this.threadId = threadId;
            Cursor cursor = fetchMessages(context, 1, 0);
            if(cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                setAddress(context, sms.getAddress());
            }
            cursor.close();
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
                    null,
                    selection,
                    selectionArgs,
                    null);

            String threadId = "";
            if (cursor.moveToFirst()) {
                SMS sms = new SMS(cursor);
                threadId = sms.getThreadId();
                return threadId;
            }

            cursor.close();
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
            String strippedNumber = formattedString.replaceAll("[^0-9+]", "");
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
         *
         * @param context
         * @return byte[] : Returns the public key. Remember this is your
         * primary key (you being whomever is initiating the handshake).
         * @throws GeneralSecurityException
         * @throws IOException
         */
        public byte[] generateAgreements(Context context) throws GeneralSecurityException, IOException {
            SecurityECDH securityECDH = new SecurityECDH(context);
            PublicKey publicKey = securityECDH.generateKeyPair(context, getAddress());

            return SecurityHelpers.txAgreementFormatter(publicKey.getEncoded());
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
            byte[] peerPublicKey = Base64.decode(securityECDH.getPeerAgreementPublicKey(getAddress()),
                    Base64.DEFAULT);

            KeyPair keyPair = securityECDH.generateKeyPairFromPublicKey(peerPublicKey);
            byte[] secret = securityECDH.generateSecretKey(peerPublicKey, getAddress());
            securityECDH.securelyStoreSecretKey(getAddress(), secret);

            return keyPair.getPublic().getEncoded();
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
            byte[] encryptedContent = SecurityECDH.encryptAES(data.getBytes(StandardCharsets.UTF_8),
                    getSecretKey(context));
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
                    Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID,
                    Telephony.TextBasedSmsColumns.TYPE};

            Cursor smsMessagesCursor = context.getContentResolver().query(
                    SMS_CONTENT_URI,
                    selection,
                    Telephony.TextBasedSmsColumns.THREAD_ID + "=?",
                    new String[]{threadId},
                    constrains);

            return smsMessagesCursor;
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

    public static class SMSJsonEntity {
        public String type;
        public List<SMS> smsList = new ArrayList<>();

        public void setSmsList(Cursor cursor) {
            if(cursor.moveToFirst()) {
                do {
                    SMS sms = new SMS(cursor);
                    smsList.add(sms);
                } while(cursor.moveToNext());
            }
        }
    }
}
