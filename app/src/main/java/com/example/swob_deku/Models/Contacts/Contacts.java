package com.example.swob_deku.Models.Contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Ignore;

public class Contacts {

    @Ignore
    public int type;

    public long id;

    public String number = "";
    public String contactName = "";

    Context context;

    public static final int TYPE_OLD_CONTACT = 1;
    public static final int TYPE_NEW_CONTACT = 2;

    public Contacts(Context context, long id, String contactName, @NonNull String number){
        this.id = id;
        this.contactName = contactName;
        this.context = context;
        this.type = TYPE_OLD_CONTACT;
        this.number = number;

    }

    public Contacts(){ }

    private void getPhoneNumber() {
        String phoneNumber = "";
        ContentResolver cr = context.getContentResolver();
        Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(id)},
                null);
        if (phoneCursor.moveToFirst()) {
            this.number = phoneCursor.getString(
                    phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }
        phoneCursor.close();
    }

    public static Cursor filterContacts(Context context, String filter) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL AND " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " <> '' AND (" +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE '%" + filter + "%' OR " +
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE '%" + filter + "%')";
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    public static Cursor filterPhonebookContactsByName(Context context, String filter) {
//        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                uri,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " like ?",
                new String[]{ "%" + filter + "%"},
                null);
        return cursor;
    }

    public static Cursor getPhonebookContacts(Context context) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL AND " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " <> ''";
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        return context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
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
        cursor.close();

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
