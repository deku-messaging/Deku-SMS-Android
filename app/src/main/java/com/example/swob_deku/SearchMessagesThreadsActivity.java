package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Models.Messages.MessagesSearchViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS.SMS;

import java.util.List;

public class SearchMessagesThreadsActivity extends AppCompatActivity {

    MessagesSearchViewModel messagesSearchViewModel;
    MutableLiveData<String> searchString = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_messages_threads);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.search_messages_toolbar);
        myToolbar.setTitle(R.string.search_messages_text);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        messagesSearchViewModel = new ViewModelProvider(this).get(
                MessagesSearchViewModel.class);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, true, searchString.getValue());

        SearchView searchView = findViewById(R.id.search_view_input);
        CustomContactsCursorAdapter customContactsCursorAdapter = new CustomContactsCursorAdapter(getApplicationContext(),
                Contacts.getPhonebookContacts(getApplicationContext()), 0);
        searchView.setSuggestionsAdapter(customContactsCursorAdapter);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = (Cursor) customContactsCursorAdapter.getItem(position);
                String address = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                Intent intent = new Intent(getApplicationContext(), SMSSendActivity.class);
                intent.putExtra(SMS.SMSMetaEntity.ADDRESS, address);
                startActivity(intent);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchString.setValue(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                customContactsCursorAdapter.changeCursor(Contacts.filterContacts(getApplicationContext(), newText));
                return true;
            }
        });

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.search_results_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        searchString.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                messagesSearchViewModel.informChanges(getApplicationContext(), s);
            }
        });

        messagesSearchViewModel.getMessages(getApplicationContext(), searchString.getValue()).observe(this,
                new Observer<List<SMS>>() {
                    @Override
                    public void onChanged(List<SMS> smsList) {
                        if(!searchString.getValue().isEmpty() && smsList.isEmpty())
                            findViewById(R.id.search_nothing_found).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.search_nothing_found).setVisibility(View.GONE);
                        messagesThreadRecyclerAdapter.submitList(smsList, searchString.getValue());
                        messagesThreadRecyclerAdapter.notifyDataSetChanged();
                    }
                });
    }

    public static class CustomContactsCursorAdapter extends CursorAdapter {

        public CustomContactsCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            TextView textView = (TextView) view;
            textView.setText(name);
        }
    }
}