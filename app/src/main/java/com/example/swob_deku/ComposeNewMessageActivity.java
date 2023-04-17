package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Contacts.ContactsRecyclerAdapter;
import com.example.swob_deku.Models.Contacts.ContactsViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ComposeNewMessageActivity extends AppCompatActivity {

    ContactsViewModel contactsViewModel;
    RecyclerView contactsRecyclerView;
    ContactsRecyclerAdapter contactsRecyclerAdapter;

    MutableLiveData<String> contactFilter = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_new_message);

        Toolbar toolbar = (Toolbar) findViewById(R.id.compose_new_message_toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        ab.setTitle(getString(R.string.search_title));

        contactsViewModel = new ViewModelProvider(this).get(
                ContactsViewModel.class);

        contactsRecyclerAdapter = new ContactsRecyclerAdapter( this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        contactsRecyclerView = findViewById(R.id.compose_new_message_contact_list_recycler_view);
        contactsRecyclerView.setLayoutManager(linearLayoutManager);
        contactsRecyclerView.setAdapter(contactsRecyclerAdapter);

        contactsViewModel.getContacts(getApplicationContext()).observe(this, new Observer<List<Contacts>>() {
            @Override
            public void onChanged(List<Contacts> contacts) {
                contactsRecyclerAdapter.submitList(contacts);
            }
        });

        TextInputEditText textInputEditText = findViewById(R.id.compose_new_message_to);
        textInputEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(getLocalClassName(), "keycode: " + keyCode);
                Log.d(getLocalClassName(), "event: " + event.toString() + "\n");
                if(event.getAction() == KeyEvent.ACTION_UP) {
                    Editable editable = textInputEditText.getText();
                    if (editable != null) {
                        contactFilter.setValue(editable.toString());
                        return true;
                    }
                }
                return false;
            }
        });

        contactFilter.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                Log.d(getLocalClassName(), "Yep should change search");
            }
        });
    }
}
