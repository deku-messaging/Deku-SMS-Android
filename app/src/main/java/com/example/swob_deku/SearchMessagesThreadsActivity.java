package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.swob_deku.Models.Messages.MessagesSearchViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.MessagesThreadViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class SearchMessagesThreadsActivity extends AppCompatActivity {

    // TODO: custom search with startIcon being up button
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
                this, R.layout.messages_threads_layout, true, searchString.getValue());

        TextInputEditText searchTextInput = findViewById(R.id.new_gateway_client_url_input);
        searchTextInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView searchView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String searchInput = searchView.getText().toString();
                    searchString.setValue(searchInput);
//                    messagesThreadRecyclerAdapter.setSearchString(searchInput);

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

                    findViewById(R.id.search_results_recycler_view).requestFocus();

                    return true;
                }
                return false;
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
//                        if(smsList.size() < 1 )
//                            findViewById(R.id.no_gateway_server_added).setVisibility(View.VISIBLE);
                        messagesThreadRecyclerAdapter.submitList(smsList);
//                        messagesThreadRecyclerView.smoothScrollToPosition(0);
                    }
                });
    }
}