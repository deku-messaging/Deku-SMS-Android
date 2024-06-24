package com.afkanerd.deku.DefaultSMS.Modals

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.afkanerd.deku.DefaultSMS.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class FailedMessageRetryModal(private val onClickedRunnable: Runnable) :
    BottomSheetDialogFragment(R.layout.failed_messages_modal_sheet) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<View>(R.id.conversation_failed_messages_modal)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        view.findViewById<MaterialButton>(R.id.conversation_failed_message_retry_btn)
            .setOnClickListener {
                dismiss()
                onClickedRunnable.run()
            }
    }
}