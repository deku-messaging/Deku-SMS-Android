package com.afkanerd.deku.Router.GatewayServers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import com.afkanerd.deku.DefaultSMS.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GatewayServerAddModelFragment( val bottomSheetViewLayout: Int)
    : BottomSheetDialogFragment() {

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_modalsheet_gateway_server, container,
                false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewStub = view.findViewById<ViewStub>(R.id.gateway_server_add_include_layout)
//        viewStub.layoutResource = R.layout.activity_gateway_server_add
        viewStub.layoutResource = bottomSheetViewLayout
        val inflatedView = viewStub.inflate()
        val bottomSheet = inflatedView.findViewById<View>(R.id.activity_gateway_server_add_modal)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}