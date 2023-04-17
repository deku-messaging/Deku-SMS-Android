package com.example.swob_deku.Models.Contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.nio.channels.MulticastChannel;
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

    public void loadContacts(){
        Cursor cursor = Contacts.getPhonebookContacts(context);
        List<Contacts> contactsList = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                int displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME);
//                int addressIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER);

                String displayName = String.valueOf(cursor.getString(displayNameIndex));
//                String address = String.valueOf(cursor.getString(addressIndex));

                contactsList.add(new Contacts(displayName, displayName));
            } while(cursor.moveToNext());
        }
        cursor.close();
        contactsMutableLiveData.setValue(contactsList);
    }
}
