package com.afkanerd.deku.Router.GatewayServers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Router.SMTP
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText

class GatewayServerAddModelFragment( val bottomSheetViewLayout: Int,
                                     val gatewayServer: GatewayServer?)
    : BottomSheetDialogFragment() {

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public lateinit var runnable: Runnable

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

        gatewayServer?.let { editGatewayServer(view, gatewayServer) }

        var materialButton: MaterialButton = inflatedView
                .findViewById<MaterialButton>(R.id.gateway_server_customization_save_btn)
        materialButton.setOnClickListener {
            runnable.run()
        }
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    private fun editGatewayServer(view: View, gatewayServer: GatewayServer) {
        if(gatewayServer.protocol == SMTP.PROTOCOL)
            editSMTP(view, gatewayServer)
        else editHTTP(view, gatewayServer)
    }

    private fun editSMTP(view: View, gatewayServer: GatewayServer) {
        val textInputHost = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_host_input)
        textInputHost.setText(gatewayServer.smtp.host)

        val textInputUsername = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_username_input)
        textInputUsername.setText(gatewayServer.smtp.username)

        val textInputPassword = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_password_input)
        textInputPassword.setText(gatewayServer.smtp.password)

        val textInputPort = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_port_input)
        textInputPort.setText(gatewayServer.smtp.port.toString())

        val textInputFrom = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_from_input)
        textInputFrom.setText(gatewayServer.smtp.from)

        val textInputRecipient = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_recipient_input)
        textInputRecipient.setText(gatewayServer.smtp.recipient)

        val textInputSubject = view
                .findViewById<TextInputEditText>(R.id.gateway_server_add_smtp_subject_input)
        textInputSubject.setText(gatewayServer.smtp.subject)

        val materialCheckBoxBase64: MaterialCheckBox =
                view.findViewById<MaterialCheckBox>(R.id.add_gateway_data_format_base64)
        materialCheckBoxBase64.isChecked = gatewayServer.format == GatewayServer.BASE64_FORMAT
    }

    private fun editHTTP(view: View, gatewayServer: GatewayServer) {
        val textInputEditTextUrl = view.findViewById<TextInputEditText>(R.id.new_gateway_server_url_input)
        textInputEditTextUrl.setText(gatewayServer.url)

        val textInputEditTextTag = view.findViewById<TextInputEditText>(R.id.new_gateway_server_tag_input)
        textInputEditTextTag.setText(gatewayServer.getTag())

        val materialCheckBoxBase64: MaterialCheckBox =
                view.findViewById<MaterialCheckBox>(R.id.add_gateway_data_format_base64)
        materialCheckBoxBase64.isChecked = gatewayServer.format == GatewayServer.BASE64_FORMAT
    }
}