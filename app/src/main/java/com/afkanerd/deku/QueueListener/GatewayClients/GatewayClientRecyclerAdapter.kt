package com.afkanerd.deku.QueueListener.GatewayClients

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.Commons.Helpers
import com.afkanerd.deku.DefaultSMS.Models.ServiceHandler
import com.afkanerd.deku.DefaultSMS.R

class GatewayClientRecyclerAdapter :
    RecyclerView.Adapter<GatewayClientRecyclerAdapter.ViewHolder>() {
    private val mDiffer: AsyncListDiffer<GatewayClient> = AsyncListDiffer( this,
        GatewayClient.DIFF_CALLBACK )

    var onSelectedListener: MutableLiveData<GatewayClient> = MutableLiveData()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.gateway_client_listing_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gatewayClient = mDiffer.currentList[position]
        holder.bind(gatewayClient, onSelectedListener)
    }


    fun submitList(gatewayClientList: List<GatewayClient>?) {
        mDiffer.submitList(gatewayClientList)
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var url: TextView = itemView.findViewById(R.id.gateway_client_url)
        private var virtualHost: TextView = itemView.findViewById(R.id.gateway_client_virtual_host)
        private var friendlyName: TextView = itemView.findViewById(R.id.gateway_client_friendly_name_text)
        private var date: TextView = itemView.findViewById(R.id.gateway_client_date)
        private var username: TextView = itemView.findViewById(R.id.gateway_client_username)
        private var connectionStatus = itemView.findViewById<TextView>(R.id.gateway_client_connection_status)

        private var cardView: CardView = itemView.findViewById(R.id.gateway_client_card)

        fun bind(gatewayClient: GatewayClient, onSelectedListener: MutableLiveData<GatewayClient>) {
            val urlBuilder = gatewayClient.protocol + "://" +
                    gatewayClient.hostUrl + ":" +
                    gatewayClient.port

            url.text = urlBuilder
            virtualHost.text = gatewayClient.virtualHost
            friendlyName.text = gatewayClient.friendlyConnectionName
            username.text = gatewayClient.username
            connectionStatus.text = gatewayClient.connectionStatus

            val date = Helpers.formatDate(itemView.context, gatewayClient.date)
            this.date.text = date

            if (gatewayClient.friendlyConnectionName.isNullOrEmpty())
                friendlyName.visibility = View.GONE
            else friendlyName.text = gatewayClient.friendlyConnectionName

           cardView.setOnClickListener { onSelectedListener.value = gatewayClient }
        }
    }

    companion object {
        const val ADAPTER_POSITION: String = "ADAPTER_POSITION"
    }
}
