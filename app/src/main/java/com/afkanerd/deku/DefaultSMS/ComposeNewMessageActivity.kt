package com.afkanerd.deku.DefaultSMS

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toolbar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ContactsRecyclerAdapter
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ContactsViewModel
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.google.android.material.textfield.TextInputEditText
import com.google.i18n.phonenumbers.NumberParseException


class ComposeNewMessageActivity : CustomAppCompactActivity() {
    private val contactsViewModel: ContactsViewModel by viewModels()
    private lateinit var contactsRecyclerView: RecyclerView
    private val contactsRecyclerAdapter: ContactsRecyclerAdapter = ContactsRecyclerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_new_message)

        val myToolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.compose_new_message_toolbar)
        setSupportActionBar(myToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.compose_new_message_title)

        val linearLayoutManager = LinearLayoutManager( this,
            LinearLayoutManager.VERTICAL, false )

        contactsRecyclerView = findViewById(R.id.compose_new_message_contact_list_recycler_view)
        contactsRecyclerView.setLayoutManager(linearLayoutManager)
        contactsRecyclerView.setAdapter(contactsRecyclerAdapter)

        contactsViewModel.getContacts(applicationContext).observe(this, Observer {
            contactsRecyclerAdapter.submitList(it)
        })

        val textInputEditText = findViewById<TextInputEditText>(R.id.compose_new_message_to)
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                ThreadingPoolExecutor.executorService.execute {
                    try {
                        if(s.isNotEmpty())
                            contactsViewModel.filterContact(applicationContext, s.toString())
                        else contactsViewModel.filterContact(applicationContext, "")
                    } catch (e: NumberParseException) {
                        e.printStackTrace()
                    }
                }
            }
        })

        checkSharedContent()
    }

    private fun checkSharedContent() {
        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if ("text/plain" == intent.type) {
                val sharedSMS = intent.getStringExtra(Intent.EXTRA_TEXT)
                contactsRecyclerAdapter!!.setSharedMessage(sharedSMS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.compose_new_message_to).requestFocus()
    }
}