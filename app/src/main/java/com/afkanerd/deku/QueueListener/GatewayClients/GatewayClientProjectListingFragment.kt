package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Modules.ThreadingPoolExecutor

class GatewayClientProjectListingFragment(val gatewayClientId: Long) :
        Fragment(R.layout.fragment_modalsheet_gateway_client_project_listing_layout) {
    private val gatewayClientProjectListingViewModel :
            GatewayClientProjectListingViewModel by viewModels()

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,
                               savedInstanceState: Bundle? ): View? {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linearLayoutManager = LinearLayoutManager(view.context)
        val recyclerView = view
            .findViewById<RecyclerView>(R.id.gateway_client_project_listing_recycler_view)

        val gatewayClientProjectListingRecyclerAdapter = GatewayClientProjectListingRecyclerAdapter()
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = gatewayClientProjectListingRecyclerAdapter

        gatewayClientProjectListingRecyclerAdapter.onSelectedLiveData.observe(viewLifecycleOwner,
            Observer {
                it?.let {
                    gatewayClientProjectListingRecyclerAdapter.onSelectedLiveData = MutableLiveData()
                    showAddGatewayClientModal(it)
                }
            })

        gatewayClientProjectListingViewModel.get(requireContext(), gatewayClientId)
                .observe(viewLifecycleOwner, Observer {
                    gatewayClientProjectListingRecyclerAdapter.mDiffer.submitList(it)
                    if (it.isNullOrEmpty())
                        view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                            .visibility = View.VISIBLE
                    else view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                        .visibility = View.GONE
                })

        ThreadingPoolExecutor.executorService.execute {
            val gatewayClientLiveData = Datastore.getDatastore(view.context).gatewayClientDAO()
                    .fetchLiveData(gatewayClientId)
            activity?.runOnUiThread {
                gatewayClientLiveData.observe(viewLifecycleOwner, Observer {
                    gatewayClient = it
                    activity?.invalidateOptionsMenu()
                })
            }
        }
    }

    private var gatewayClient = GatewayClient()

    private fun showAddGatewayClientModal(gatewayClientProjects: GatewayClientProjects? = null) {
        val fragmentManager: FragmentManager = activity?.supportFragmentManager!!
        val fragmentTransaction = fragmentManager.beginTransaction()
        val gatewayClientProjectAddModalFragment =
            GatewayClientProjectAddModalFragment(gatewayClientProjectListingViewModel,
                gatewayClientId, gatewayClientProjects)
        fragmentTransaction.add(gatewayClientProjectAddModalFragment,
                "gateway_client_add_edit")
        fragmentTransaction.show(gatewayClientProjectAddModalFragment)
        fragmentTransaction.commit()
    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.gateway_client_project_listing_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if(gatewayClient.activated) {
            menu.findItem(R.id.gateway_client_project_disconnect)
                    .setVisible(true)
        } else {
            menu.findItem(R.id.gateway_client_project_connect)
                    .setVisible(true)
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.gateway_client_project_add) {
            showAddGatewayClientModal()
            return true
        }
        if (item.itemId == R.id.gateway_client_edit) {
            val intent = Intent(requireContext(), GatewayClientAddActivity::class.java)
            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, gatewayClientId)

            startActivity(intent)
            return true
        }
        if (item.itemId == R.id.gateway_client_project_connect) {
            gatewayClient.activated = true
            ThreadingPoolExecutor.executorService.execute {
                Datastore.getDatastore(requireContext()).gatewayClientDAO().update(gatewayClient)
            }
            GatewayClientHandler.startListening(requireContext(), gatewayClient)
            return true
        }
//        if (item.itemId == R.id.gateway_client_project_disconnect) {
//            sharedPreferences!!.edit().remove(id.toString())
//                .apply()
//            finish()
//            return true
//        }
        return false
    }
}