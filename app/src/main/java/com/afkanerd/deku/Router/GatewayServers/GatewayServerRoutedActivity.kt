package com.afkanerd.deku.Router.GatewayServers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Router.Models.RouterHandler
import com.afkanerd.deku.Router.Models.RouterItem

class GatewayServerRoutedActivity : CustomAppCompactActivity() {
    private val gatewayServerRouterViewModel: GatewayServerRouterViewModel by viewModels()
    private lateinit var routedMessageRecyclerView: RecyclerView
    private lateinit var gatewayServerRouterRecyclerAdapter: GatewayServerRouterRecyclerAdapter

    private var actionMode: ActionMode? = null
    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router)

        toolbar = findViewById(R.id.router_activity_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = getString(R.string.settings_SMS_routing_title)

        val linearLayoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false)

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view)
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager)

        gatewayServerRouterRecyclerAdapter = GatewayServerRouterRecyclerAdapter()
        gatewayServerRouterRecyclerAdapter.setHasStableIds(true)

        routedMessageRecyclerView.setAdapter(gatewayServerRouterRecyclerAdapter)
        gatewayServerRouterViewModel.getMessages(applicationContext).observe(this, Observer {
            sortAndSubmit(applicationContext, it)
        })

        gatewayServerRouterRecyclerAdapter.selectedItems.observe(this, Observer {
        })
    }

    private fun sortAndSubmit(context: Context, workInfoList: List<WorkInfo>) {
        val routerItemsList = ArrayList<Pair<RouterItem, GatewayServer>>()
        ThreadingPoolExecutor.executorService.execute {
            workInfoList.sortedByDescending { RouterHandler.workInfoParser(it).first }
                    .forEach { workInfo ->
                val workInfoPair = RouterHandler.workInfoParser(workInfo)
                println("${workInfoPair.first}:${workInfoPair.second}")
                Datastore.getDatastore(context).gatewayServerDAO()[workInfoPair.second]
                        .let {gatewayServer->
                            RouterItem(Datastore.getDatastore(context).conversationDao()
                                    .getMessage(workInfoPair.first)).let {
                                it.routingUniqueId = workInfo.id.toString()
                                it.routingStatus = RouterHandler.reverseState(context,
                                        workInfo.state)
                                routerItemsList.add(Pair(it, gatewayServer))
                            }
                        }
            }
            runOnUiThread {
                gatewayServerRouterRecyclerAdapter.mDiffer.submitList(routerItemsList)
                if (routerItemsList.isNotEmpty())
                    findViewById<View>(R.id.router_no_showable_messages_text).visibility = View.GONE
                else {
                    findViewById<View>(R.id.router_no_showable_messages_text).visibility = View.VISIBLE
                    routedMessageRecyclerView.smoothScrollToPosition(0)
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gateway_server_routed_list_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.router_list_gateways_menu_item -> {
                startActivity(Intent(this, GatewayServerListingActivity::class.java))
                return true
            }
            R.id.gateway_server_menu_settings -> {
                startActivity(Intent(this, GatewayServerSettingsActivity::class.java))
                return true
            }
        }
        return false
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.gateway_server_routed_menu_items_selected, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false // Return false if nothing is done.
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
        }
    }
}