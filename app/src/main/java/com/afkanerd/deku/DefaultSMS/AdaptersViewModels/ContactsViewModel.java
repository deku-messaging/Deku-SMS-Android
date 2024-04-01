package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.Contacts;
import com.google.i18n.phonenumbers.NumberParseException;

import java.util.ArrayList;
import java.util.List;

public class ContactsViewModel extends ViewModel {

    MutableLiveData<List<Contacts>> contactsMutableLiveData;

    public MutableLiveData<List<Contacts>> getContacts(Context context) {
        if(contactsMutableLiveData == null) {
            contactsMutableLiveData = new MutableLiveData<>();
            loadContacts(context);
        }

        return contactsMutableLiveData;
    }

    public void filterContact(Context context, String details) throws NumberParseException {
        List<Contacts> contactsList = new ArrayList<>();
        if(details.isEmpty()) {
            loadContacts(context);
            return;
        }

        Cursor cursor = Contacts.filterContacts(context, details);
        if(cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID);
                int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String displayName = String.valueOf(cursor.getString(displayNameIndex));
                long id = cursor.getLong(idIndex);
                String number = String.valueOf(cursor.getString(numberIndex));

                contactsList.add(new Contacts(context,  id, displayName, number));
            } while(cursor.moveToNext());
        }
        cursor.close();

        if(contactsList.isEmpty() && PhoneNumberUtils.isWellFormedSmsAddress(details)) {
            Contacts contacts = new Contacts();
            contacts.contactName = "Send to " + details;
            contacts.number = details;
            contacts.type = Contacts.TYPE_NEW_CONTACT;
            contactsList.add(contacts);
        }
        contactsMutableLiveData.postValue(contactsList);
    }


    public void loadContacts(Context context){
        Cursor cursor = Contacts.getPhonebookContacts(context);
        List<Contacts> contactsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID);
                int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String displayName = String.valueOf(cursor.getString(displayNameIndex));
                long id = cursor.getLong(idIndex);
                String number = String.valueOf(cursor.getString(numberIndex));

                contactsList.add(new Contacts(context, id, displayName, number));
            } while(cursor.moveToNext());
        }
        cursor.close();
        contactsMutableLiveData.postValue(contactsList);
    }
}
