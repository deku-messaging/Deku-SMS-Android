package com.afkanerd.deku.DefaultSMS.Fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.E2EE.E2EEHandler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.concurrent.ThreadPoolExecutor

class ModalSheetFragment(var threadedConversations: ThreadedConversations) : BottomSheetDialogFragment() {

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_modalsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<View>(R.id.conversations_bottom_sheet_view_id)

        // Get the BottomSheetBehavior instance
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        view.findViewById<View>(R.id.conversation_secure_request_agree_read_more_btn)
                .setOnClickListener(OnClickListener { clickPrivacyPolicy(it) })
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    private fun clickPrivacyPolicy(view: View?) {
        val url = getString(R.string.conversations_secure_conversation_request_information_deku_encryption_link)
        val shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        view?.context?.startActivity(shareIntent)
    }

    private fun agreeToSecure() {
        var keystoreAlias = E2EEHandler.deriveKeystoreAlias(threadedConversations.address, 0)
        ThreadingPoolExecutor.executorService.execute {
            if (threadedConversations.isSelf) {
                keystoreAlias = E2EEHandler.buildForSelf(keystoreAlias)
            }
            val keystorePair: Pair<String, ByteArray> =
                    E2EEHandler.buildForEncryptionRequest(context,
                            threadedConversations.address, keystoreAlias)
            val transmissionKey: ByteArray = E2EEHandler.extractTransmissionKey(keystorePair.second)
            E2EEHandler.insertNewAgreementKeyDefault(context, transmissionKey, keystoreAlias)
            val tc: ThreadedConversations =
                    Datastore.datastore.threadedConversationsDao()
                            .get(threadedConversations.thread_id)
            tc.isIs_secured = true
            Datastore.datastore.threadedConversationsDao().update(tc);
            threadedConversations = tc
        }
    }
}