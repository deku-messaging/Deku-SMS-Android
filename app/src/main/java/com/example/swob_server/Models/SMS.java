package com.example.swob_server.Models;

import android.database.Cursor;
import android.provider.Telephony;

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

    public String getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    String address = new String();
    String threadId = new String();
    String dateReceived= new String();
    String type;

    public SMS(Cursor cursor) {
        int bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY);
        int addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS);
        int threadIdIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID);
        int dateReceivedIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE);
        int typeIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE);

        this.body =  String.valueOf(cursor.getString(bodyIndex));
        this.address =  String.valueOf(cursor.getString(addressIndex));
        this.threadId =  String.valueOf(cursor.getString(threadIdIndex));
        this.dateReceived =  String.valueOf(cursor.getString(dateReceivedIndex));
        this.type =  String.valueOf(cursor.getString(typeIndex));
    }

}
