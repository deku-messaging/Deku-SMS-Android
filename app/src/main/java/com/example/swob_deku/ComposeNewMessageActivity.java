package com.example.swob_deku;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Contacts.ContactsRecyclerAdapter;
import com.example.swob_deku.Models.Contacts.ContactsViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;

import java.util.List;

public class ComposeNewMessageActivity extends AppCompatActivity {

    ContactsViewModel contactsViewModel;
    RecyclerView contactsRecyclerView;
    ContactsRecyclerAdapter contactsRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_new_message);

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
    }
}