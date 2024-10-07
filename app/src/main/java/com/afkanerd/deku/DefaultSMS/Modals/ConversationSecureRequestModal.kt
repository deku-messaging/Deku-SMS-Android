package com.afkanerd.deku.DefaultSMS.Modals

import android.os.Bundle
import android.view.View
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler
import com.afkanerd.deku.DefaultSMS.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ConversationSecureRequestModal(private val requestCallback: Runnable) :
    BottomSheetDialogFragment(R.layout.fragment_modal_secure_request){

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<View>(R.id.conversation_secure_request_modal)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        view.findViewById<MaterialButton>(R.id.conversation_secure_request_btn).setOnClickListener {
            it.isEnabled = false
            requestCallback.run()
            activity?.recreate()
            dismiss()
        }
    }
}