package com.example.swob_deku.Models.Contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Commons.Helpers;
import com.google.i18n.phonenumbers.NumberParseException;

import java.util.ArrayList;
import java.util.List;

public class ContactsViewModel extends ViewModel {

    Context context;
    MutableLiveData<List<Contacts>> contactsMutableLiveData;

    public MutableLiveData<List<Contacts>> getContacts(Context context) {
        this.context = context;

        if(contactsMutableLiveData == null) {
            contactsMutableLiveData = new MutableLiveData<>();
            loadContacts();
        }

        return contactsMutableLiveData;
    }

    public void filterContact(String details) throws NumberParseException {
        List<Contacts> contactsList = new ArrayList<>();
        if(details.isEmpty()) {
            loadContacts();
            return;
        }

        if(PhoneNumberUtils.isWellFormedSmsAddress(details)) {
            contactsList = filterContactsForPhonenumber(details);
            if(contactsList.isEmpty()) {
                Contacts contacts = new Contacts();
                contacts.contactName = "Send to " + details;
                contacts.number = details;
                contacts.type = Contacts.TYPE_NEW_CONTACT;
                contactsList.add(contacts);
            }
        } else {
            Cursor cursor = Contacts.filterPhonebookContactsByName(context, details);
            Log.d(getClass().getName(), "Found filter by name: " + cursor.getCount() + ":" + details);
            if(cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
                    int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);

                    String displayName = String.valueOf(cursor.getString(displayNameIndex));
                    long id = cursor.getLong(idIndex);

                    contactsList.add(new Contacts(context,  id, displayName));
                } while(cursor.moveToNext());
            }
            cursor.close();
        }

        contactsMutableLiveData.setValue(contactsList);
    }

    private List<Contacts> filterContactsForPhonenumber(String number) throws NumberParseException {
        Cursor cursor = Contacts.getPhonebookContacts(context);
        List<Contacts> contactsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
                int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);

                String displayName = String.valueOf(cursor.getString(displayNameIndex));
                long id = cursor.getLong(idIndex);

                Contacts contacts = new Contacts(context, id, displayName);
                if(Helpers.formatPhoneNumbers(contacts.number).contains(Helpers.formatPhoneNumbers(number)))
                    contactsList.add(new Contacts(context, id, displayName));
            } while(cursor.moveToNext());
        }

        cursor.close();
        return contactsList;
    }

    public void loadContacts(){
        Cursor cursor = Contacts.getPhonebookContacts(context);
        List<Contacts> contactsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
                int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
//                int addressIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER);

                String displayName = String.valueOf(cursor.getString(displayNameIndex));
                long id = cursor.getLong(idIndex);
//                String address = String.valueOf(cursor.getString(addressIndex));

                contactsList.add(new Contacts(context, id, displayName));
            } while(cursor.moveToNext());
        }
        cursor.close();
        contactsMutableLiveData.setValue(contactsList);
    }
}
