package com.afkanerd.deku.DefaultSMS.Fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.afkanerd.deku.DefaultSMS.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ConversationsContactModalFragment(val contactName: String, val address: String) :
        BottomSheetDialogFragment() {

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.layout_conversation_contact_card_modalsheet, container,
                false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = view.findViewById<View>(R.id.conversation_contact_sheet_modal)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        view.findViewById<TextView>(R.id.conversation_contact_modal_contact_text)
                .text = contactName

        view.findViewById<TextView>(R.id.conversation_contact_modal_details_address)
                .text = address

        view.findViewById<ImageButton>(R.id.conversation_contact_modal_copy_icon)
                .setOnClickListener { copy() }
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    private fun copy() {
        val clipData = ClipData.newPlainText(contactName, address)
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)

        Toast.makeText(context, getString(R.string.conversation_copied), Toast.LENGTH_SHORT).show()
    }
}