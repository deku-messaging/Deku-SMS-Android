package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientProjectAddModalFragment

class GatewayClientProjectListingRecyclerAdapter :
    RecyclerView.Adapter<GatewayClientProjectListingRecyclerAdapter.ViewHolder>() {
    val mDiffer: AsyncListDiffer<GatewayClientProjects> = AsyncListDiffer(
        this, GatewayClientProjects.DIFF_CALLBACK )

    var onSelectedLiveData: MutableLiveData<GatewayClientProjects> = MutableLiveData()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_gateway_client_project_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gatewayClientProjects = mDiffer.currentList[position]
        holder.bind(gatewayClientProjects, onSelectedLiveData)
    }

    override fun getItemCount(): Int { return mDiffer.currentList.size }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var cardView: CardView = itemView.findViewById(R.id.gateway_client_project_listing_card)

        private var projectNameTextView: TextView =
            itemView.findViewById(R.id.gateway_client_project_listing_project_name)

        private var projectBinding1TextView: TextView =
            itemView.findViewById(R.id.gateway_client_project_listing_project_binding1)

        private var projectBinding2TextView: TextView =
            itemView.findViewById(R.id.gateway_client_project_listing_project_binding2)

        fun bind(gatewayClientProjects: GatewayClientProjects,
                 onSelectedLiveData: MutableLiveData<GatewayClientProjects>) {
            projectNameTextView.text = gatewayClientProjects.name
            projectBinding1TextView.text = gatewayClientProjects.binding1Name
            projectBinding2TextView.text = gatewayClientProjects.binding2Name

            cardView.setOnClickListener { onSelectedLiveData.value = gatewayClientProjects }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }
}
