package com.example.swob_deku.Models.SMS;

import android.database.Cursor;
import android.provider.Telephony;

import androidx.annotation.Nullable;

public class SMS {
    // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#constants_1

    String body = new String();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    String address = new String();
    String threadId = "-1";
    String date = new String();
    String type;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int messageCount = -1;

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    String errorCode;
    int statusCode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String isRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public String id;

    public String read;

    public String routerStatus = new String();

    public Boolean datesOnly = false;

    public Boolean isDatesOnly() {
        return this.datesOnly;
    }

    public String getRouterStatus() {
        return this.routerStatus;
    }

    public void setRouterStatus(String routerStatus) {
        this.routerStatus = routerStatus;
    }

    public SMS(String dates) {
        this.date = dates;
        this.datesOnly = true;
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

        this.type =  String.valueOf(cursor.getString(typeIndex));
        this.body = String.valueOf(cursor.getString(bodyIndex));
        this.address = String.valueOf(cursor.getString(addressIndex));
        this.threadId = String.valueOf(cursor.getString(threadIdIndex));
        this.date = String.valueOf(cursor.getString(dateIndex));


        if(idIndex > -1 ) {
            this.id = String.valueOf(cursor.getString(idIndex));
        }

        if(readIndex > -1 ) {
            this.read = String.valueOf(cursor.getString(readIndex));
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
                    sms.getStatusCode() == this.statusCode &&
                    sms.address.equals(this.address) &&
                    sms.body.equals(this.body) &&
                    sms.date.equals(this.date);
        }
        return false;
    }
}
