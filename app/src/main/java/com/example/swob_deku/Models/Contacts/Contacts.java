package com.example.swob_deku.Models.Contacts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.example.swob_deku.BuildConfig;
import com.example.swob_deku.Models.SMS.SMS;

public class Contacts {

    public String number;
    public String contactName;

    public Contacts(String number, String contactName){
        this.number = number;
        this.contactName = contactName;
    }

    public static Cursor getPhonebookContacts(Context context) {
//        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);
        return cursor;
    }

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

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj != null) {
            Contacts contacts = (Contacts) obj;
            return contacts.number.equals(this.number) &&
                    contacts.contactName.equals(this.contactName);
        }
        return false;
    }

    public static final DiffUtil.ItemCallback<Contacts> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Contacts>() {
                @Override
                public boolean areItemsTheSame(@NonNull Contacts oldItem, @NonNull Contacts newItem) {
                    return oldItem.number.equals(newItem.number);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Contacts oldItem, @NonNull Contacts newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
