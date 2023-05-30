package com.example.swob_deku.Models.GatewayClients;

import static com.example.swob_deku.Models.GatewayClients.GatewayClient.DIFF_CALLBACK;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GatewayClientRecyclerAdapter extends RecyclerView.Adapter<GatewayClientRecyclerAdapter.ViewHolder>{
    private final AsyncListDiffer<GatewayClient> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    Context context;

    public GatewayClientRecyclerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.gateway_client_listing_layout, parent, false);
        return new GatewayClientRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GatewayClient gatewayClient = mDiffer.getCurrentList().get(position);

        String urlBuilder = gatewayClient.getProtocol() + "://" +
                gatewayClient.getHostUrl() + ":" +
                gatewayClient.getPort();

        holder.url.setText(urlBuilder);
        holder.virtualHost.setText(gatewayClient.getVirtualHost());
        holder.friendlyName.setText(gatewayClient.getFriendlyConnectionName());

        String date = Helpers.formatDate(context, gatewayClient.getDate());
        holder.date.setText(date);

        if(gatewayClient.getFriendlyConnectionName() == null ||
                gatewayClient.getFriendlyConnectionName().isEmpty())
            holder.friendlyName.setVisibility(View.GONE);
        else
            holder.friendlyName.setText(gatewayClient.getFriendlyConnectionName());
    }

    public void submitList(List<GatewayClient> gatewayClientList) {
        mDiffer.submitList(gatewayClientList);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView url, virtualHost, friendlyName, date;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            url = itemView.findViewById(R.id.gateway_client_url);
            virtualHost = itemView.findViewById(R.id.gateway_client_virtual_host);
            friendlyName = itemView.findViewById(R.id.gateway_client_friendly_name_text);
            date = itemView.findViewById(R.id.gateway_client_date);
        }
    }
}
