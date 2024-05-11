package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.R

class GatewayClientProjectListingFragment(val gatewayClientId: Long) : Fragment(R.layout.activity_gateway_client_project_listing) {
    var sharedPreferences: SharedPreferences? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val toolbar = view.findViewById<Toolbar>(R.id.gateway_client_project_listing_toolbar)
//        activity?.setActionBar(toolbar)
//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


//        val username = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_USERNAME)
//        val host = intent.getStringExtra(GatewayClientListingActivity.GATEWAY_CLIENT_HOST)
//        id = intent.getLongExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1)
//        sharedPreferences = getSharedPreferences(
//            GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS,
//            MODE_PRIVATE
//        )

//        val gatewayClientId : Long = arguments
//            ?.getLong(GatewayClientListingActivity.GATEWAY_CLIENT_ID, -1)!!

        val gatewayClientProjectListingViewModel :
                GatewayClientProjectListingViewModel by viewModels()

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

                    val fragmentManager: FragmentManager = activity?.supportFragmentManager!!
                    val fragmentTransaction = fragmentManager.beginTransaction()
                    val gatewayClientProjectAddModalFragment =
                        GatewayClientProjectAddModalFragment(gatewayClientProjectListingViewModel,
                            gatewayClientId, it)
                    fragmentTransaction.add(gatewayClientProjectAddModalFragment,
                        "gateway_client_add_edit")
                    fragmentTransaction.show(gatewayClientProjectAddModalFragment)
                }
            })

        gatewayClientProjectListingViewModel.get(view.context, gatewayClientId).observe(this,
            Observer {
                gatewayClientProjectListingRecyclerAdapter.mDiffer.submitList(it)
                if (it.isNullOrEmpty())
                    view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                        .visibility = View.VISIBLE
                else view.findViewById<View>(R.id.gateway_client_project_listing_no_projects)
                    .visibility = View.GONE
            })
    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.gateway_client_project_add) {
//            val intent =
//                Intent(applicationContext, GatewayClientProjectAddModalFragment::class.java)
//            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, id)
//            intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID_NEW, true)
//            startActivity(intent)
//            return true
//        }
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
//        return false
//    }
}