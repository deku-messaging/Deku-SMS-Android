package com.afkanerd.deku.QueueListener.GatewayClients

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.R

class GatewayClientListingFragment : Fragment(R.layout.fragment_gateway_client_listing) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gatewayClientViewModel: GatewayClientViewModel by viewModels()
        val gatewayClientRecyclerAdapter = GatewayClientRecyclerAdapter()

        val linearLayoutManager = LinearLayoutManager(view.context)
        val recyclerView = view.findViewById<RecyclerView>(R.id.gateway_client_listing_recycler_view)
        recyclerView.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(view.context,
                linearLayoutManager.orientation )
        recyclerView.addItemDecoration(dividerItemDecoration)

        recyclerView.adapter = gatewayClientRecyclerAdapter

        gatewayClientRecyclerAdapter.onSelectedListener.observe(viewLifecycleOwner, Observer {
            it?.let {
                gatewayClientRecyclerAdapter.onSelectedListener = MutableLiveData()

                val gatewayClientProjectListingFragment = GatewayClientProjectListingFragment(it.id)
                activity?.supportFragmentManager?.beginTransaction()
                        ?.replace( R.id.view_fragment, gatewayClientProjectListingFragment)
                        ?.setReorderingAllowed(true)
                        ?.addToBackStack(gatewayClientProjectListingFragment.javaClass.name)
                        ?.commit()
            }
        })

        gatewayClientViewModel.getGatewayClientList(view.context).observe(this,
                Observer {
                    if (it.isNullOrEmpty())
                        view.findViewById<View>(R.id.gateway_client_no_gateway_client_label)
                                .visibility = View.VISIBLE
                    gatewayClientRecyclerAdapter.submitList(it)
                })
    }
}