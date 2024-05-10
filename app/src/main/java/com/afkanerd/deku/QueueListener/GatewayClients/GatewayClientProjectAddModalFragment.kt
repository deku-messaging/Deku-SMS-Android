package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Modules.ThreadingPoolExecutor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GatewayClientProjectAddModalFragment(private val gatewayClientProjectListingViewModel:
                                           GatewayClientProjectListingViewModel,
                                           private val gatewayClientId: Long,
                                           private val gatewayClientProjects:
                                           GatewayClientProjects? = null) :
    BottomSheetDialogFragment(R.layout.fragment_modalsheet_gateway_client_project_add_edit) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getGatewayClient(view)

        val materialButton = view.findViewById<MaterialButton>(R.id.gateway_client_customization_save_btn)
        materialButton.setOnClickListener { v ->
            try {
                onSaveGatewayClientConfiguration(v)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun getGatewayClient(view: View) {
        val projectName = view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_name)
        val projectBinding =
            view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_binding_sim_1)
        val projectBinding2 =
            view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_binding_sim_2)

        val isDualSim = SIMHandler.isDualSim(view.context)
        if (isDualSim) {
            view.findViewById<View>(R.id.new_gateway_client_project_binding_sim_2_constraint)
                .visibility = View.VISIBLE
        }

        gatewayClientProjects?.let {
            activity?.runOnUiThread {
                projectName.setText(gatewayClientProjects.name)
                projectBinding.setText(gatewayClientProjects.binding1Name)
                if (isDualSim) {
                    projectBinding2.setText(gatewayClientProjects.binding2Name)
                }
            }
        }

        projectName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val projectBindings = GatewayClientHandler
                    .getPublisherDetails( view.context, s.toString())

                projectBinding.setText(projectBindings[0])
                if (projectBindings.size > 1) {
                    projectBinding2.setText(projectBindings[1])
                }
            }
        })
    }

    private fun onSaveGatewayClientConfiguration(view: View) {
        val projectName = view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_name)
        val projectBinding =
            view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_binding_sim_1)
        val projectBinding2 =
            view.findViewById<TextInputEditText>(R.id.new_gateway_client_project_binding_sim_2)
        val projectBindingConstraint =
            view.findViewById<ConstraintLayout>(R.id.new_gateway_client_project_binding_sim_2_constraint)

        if (projectName.text == null || projectName.text.toString().isEmpty()) {
            projectName.error = getString(R.string.settings_gateway_client_cannot_be_empty)
            return
        }

        if (projectBinding.text == null || projectBinding.text.toString().isEmpty()) {
            projectBinding.error = getString(R.string.settings_gateway_client_cannot_be_empty)
            return
        }

        if (projectBindingConstraint.visibility == View.VISIBLE &&
            (projectBinding2.text == null || projectBinding2.text.toString().isEmpty())) {
            projectBinding2.error = getString(R.string.settings_gateway_client_cannot_be_empty)
            return
        }

        val gatewayClientProjectsLocal = gatewayClientProjects
        gatewayClientProjectsLocal?.name = projectName.text.toString()
        gatewayClientProjectsLocal?.binding1Name = projectBinding.text.toString()
        gatewayClientProjectsLocal?.binding2Name = projectBinding2.text.toString()
        gatewayClientProjectsLocal?.gatewayClientId = gatewayClientId

        ThreadingPoolExecutor.executorService.execute {
            gatewayClientProjectListingViewModel.insert(gatewayClientProjectsLocal!!)
        }
        dismiss()
    }

    companion object {
        const val GATEWAY_CLIENT_PROJECT_ID: String = "GATEWAY_CLIENT_PROJECT_ID"
    }
}