package com.example.swob_server.Commons;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class Contacts {

    public static String retrieveContactName(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null, null);

        String displayName = phoneNumber;
        if(cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME);
            displayName = String.valueOf(cursor.getString(displayNameIndex));
        }

        return displayName;
    }
}
