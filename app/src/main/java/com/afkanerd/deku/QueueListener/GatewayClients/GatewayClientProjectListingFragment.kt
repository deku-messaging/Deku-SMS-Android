package com.afkanerd.deku.QueueListener.GatewayClients

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
import com.afkanerd.deku.DefaultSMS.R

class GatewayClientProjectListingFragment(val gatewayClientId: Long) :
        Fragment(R.layout.fragment_modalsheet_gateway_client_project_listing_layout) {
    var sharedPreferences: SharedPreferences? = null
    val gatewayClientProjectListingViewModel :
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

        gatewayClientProjectListingViewModel.get(view.context, gatewayClientId)
                .observe(viewLifecycleOwner, Observer {
                    gatewayClientProjectListingRecyclerAdapter.mDiffer.submitList(it)
                    if (it.isNullOrEmpty())
                        view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                            .visibility = View.VISIBLE
                    else view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                        .visibility = View.GONE
                })
    }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.gateway_client_project_add) {
            showAddGatewayClientModal()
            return true
        }
//        if (item.itemId == R.id.gateway_client_edit) {
//            val intent = Intent(this, GatewayClientAddActivity::class.java)
//            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, id)
//
//            startActivity(intent)
//            return true
//        }
//        if (item.itemId == R.id.gateway_client_project_connect) {
//            val gatewayClientHandler =
//                GatewayClientHandler(applicationContext)
//            Thread {
//                val gatewayClient =
//                    gatewayClientHandler.databaseConnector.gatewayClientDAO().fetch(id)
//                try {
//                    GatewayClientHandler.startListening(applicationContext, gatewayClient)
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                }
//            }.start()
//            return true
//        }
//        if (item.itemId == R.id.gateway_client_project_disconnect) {
//            sharedPreferences!!.edit().remove(id.toString())
//                .apply()
//            finish()
//            return true
//        }
        return false
    }
}