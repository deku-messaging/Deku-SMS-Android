package com.afkanerd.deku.DefaultSMS.Models.SMS;

import static com.afkanerd.deku.DefaultSMS.Commons.Helpers.getUserCountry;
import static com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler.SMS_CONTENT_URI;
import static com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler.SMS_INBOX_CONTENT_URI;
import static com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler.SMS_OUTBOX_CONTENT_URI;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.QueueListener.RMQ.RMQConnectionService;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMS implements RMQConnectionService.SmsForwardInterface, Comparable<SMS> {
    // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#constants_1

    private String body;
    private String address;
    private String threadId;
    private String date;
    private int type;
    private String errorCode;
    private int statusCode;
    private String id;
    private int read;
    private String tag;
    private int subscriptionId;
    private String MSISDN;
    private String text;

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
        this.body = cursor.getString(bodyIndex);
        this.address = cursor.getString(addressIndex);
        this.threadId = cursor.getString(threadIdIndex);
        this.date = cursor.getString(dateIndex);

        if(subscriptionIdIndex > -1)
            this.subscriptionId = cursor.getInt(subscriptionIdIndex);

        if(idIndex > -1 ) {
            this.id = cursor.getString(idIndex);
        }

        if(readIndex > -1 ) {
            this.read = cursor.getInt(readIndex);
        }

        if(threadIdIndex > -1 )
            this.threadId = cursor.getString(threadIdIndex);

        if(errorCodeIndex > -1 )
            this.errorCode = cursor.getString(errorCodeIndex);
        if(statusCodeIndex > -1 )
            this.statusCode = cursor.getInt(statusCodeIndex);

    }


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

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public String getTag() {
        return tag;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getMSISDN() {
        return MSISDN;
    }

    public void setMSISDN(String MSISDN) {
        this.MSISDN = MSISDN;
    }

    public String getText() {
        return text;
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
    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setMsisdn(String MSISDN) {
        this.MSISDN = MSISDN;
    }

    @Override
    public int compareTo(SMS sms) {
        return Long.compare(Long.parseLong(this.date), Long.parseLong(sms.getDate()));
    }
}
