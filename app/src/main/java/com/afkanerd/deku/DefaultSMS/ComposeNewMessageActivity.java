package com.afkanerd.deku.DefaultSMS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.ContactsRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.ViewModels.ContactsViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.i18n.phonenumbers.NumberParseException;

import java.util.List;

public class ComposeNewMessageActivity extends AppCompatActivity {

    ContactsViewModel contactsViewModel;
    RecyclerView contactsRecyclerView;
    ContactsRecyclerAdapter contactsRecyclerAdapter;

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
        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (s != null && !s.toString().isEmpty()) {
                            try {
                                contactsViewModel.filterContact(s.toString());
                            } catch (NumberParseException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            try {
                                contactsViewModel.filterContact("");
                            } catch (NumberParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });

        _checkSharedContent();
    }

    private void _checkSharedContent() {
        if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null) {
            if ("text/plain".equals(getIntent().getType())) {
                String sharedSMS = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                contactsRecyclerAdapter.setSharedMessage(sharedSMS);
            }
        }
    }
}
