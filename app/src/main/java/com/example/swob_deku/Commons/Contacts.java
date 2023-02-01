package com.example.swob_deku.Commons;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.swob_deku.BuildConfig;

public class Contacts {

    public static String retrieveContactName(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null, null);

        String displayName = "";
        if(cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME);
            displayName = String.valueOf(cursor.getString(displayNameIndex));
        }

        return displayName;
    }

    public static String retrieveContactPhoto(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI},
                null,
                null, null);

        String contactPhotoThumbUri = "";
        if(cursor.moveToFirst()) {
            int displayContactPhoto = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI);
            contactPhotoThumbUri = String.valueOf(cursor.getString(displayContactPhoto));
        }

        return contactPhotoThumbUri;
    }
}
