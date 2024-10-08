package com.afkanerd.deku.DefaultSMS.Modals

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations
import com.afkanerd.deku.DefaultSMS.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ConversationsSecureRequestModalSheetFragment(val contactName: String?,
                                                   private val acceptRunnable: Runnable?)
    : BottomSheetDialogFragment(R.layout.fragment_modalsheet_secure_request_layout) {

    init {
        if(contactName.isNullOrBlank() || acceptRunnable == null)
            dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<View>(R.id.conversation_secure_request_layout)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        view.findViewById<View>(R.id.conversation_secure_request_agree_read_more_btn)
                .setOnClickListener { clickKnowMoreDekuSecurity(it) }

        val requestAddress = view.findViewById<TextView>(R.id.conversation_secure_request_modal_text)
        requestAddress.text = requestAddress.text.replace(Regex("John"), contactName!!)

        view.findViewById<MaterialButton>(R.id.conversation_secure_request_agree_btn).setOnClickListener {
            acceptRunnable!!.run()
            activity?.recreate()
            dismiss()
        }
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    private fun clickKnowMoreDekuSecurity(view: View?) {
        val url = getString(R.string.conversations_secure_conversation_request_information_deku_encryption_link)
        val shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        view?.context?.startActivity(shareIntent)
    }
}