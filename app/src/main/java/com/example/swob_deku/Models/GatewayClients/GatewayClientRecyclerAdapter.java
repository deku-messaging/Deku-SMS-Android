package com.example.swob_deku.Models.GatewayClients;

import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_ID;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;
import static com.example.swob_deku.Models.GatewayClients.GatewayClient.DIFF_CALLBACK;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.GatewayClientCustomizationActivity;
import com.example.swob_deku.GatewayClientListingActivity;
import com.example.swob_deku.R;
import com.example.swob_deku.Models.ServiceHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GatewayClientRecyclerAdapter extends RecyclerView.Adapter<GatewayClientRecyclerAdapter.ViewHolder>{
    private final AsyncListDiffer<GatewayClient> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    List<ActivityManager.RunningServiceInfo> runningServiceInfoList = new ArrayList<>();

    Context context;
    public static final String ADAPTER_POSITION = "ADAPTER_POSITION";

    SharedPreferences sharedPreferences;
    public GatewayClientRecyclerAdapter(Context context) {
        this.context = context;
        runningServiceInfoList = ServiceHandler.getRunningService(context);
        sharedPreferences = context.getSharedPreferences(GATEWAY_CLIENT_LISTENERS, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.gateway_client_listing_layout, parent, false);
        return new ViewHolder(view);
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
        holder.username.setText(gatewayClient.getUsername());
        holder.connectionStatus.setText(gatewayClient.getConnectionStatus());

        String date = Helpers.formatDate(context, gatewayClient.getDate());
        holder.date.setText(date);

        if(gatewayClient.getFriendlyConnectionName() == null ||
                gatewayClient.getFriendlyConnectionName().isEmpty())
            holder.friendlyName.setVisibility(View.GONE);
        else
            holder.friendlyName.setText(gatewayClient.getFriendlyConnectionName());

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GatewayClientCustomizationActivity.class);
                intent.putExtra(GATEWAY_CLIENT_ID, gatewayClient.getId());
                context.startActivity(intent);
            }
        });
    }


    public void submitList(List<GatewayClient> gatewayClientList) {
        mDiffer.submitList(gatewayClientList);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView url, virtualHost, friendlyName, date, username, connectionStatus;

        CardView cardView;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            url = itemView.findViewById(R.id.gateway_client_url);
            virtualHost = itemView.findViewById(R.id.gateway_client_virtual_host);
            friendlyName = itemView.findViewById(R.id.gateway_client_friendly_name_text);
            date = itemView.findViewById(R.id.gateway_client_date);
            cardView = itemView.findViewById(R.id.gateway_client_card);
            username = itemView.findViewById(R.id.gateway_client_username);
            username = itemView.findViewById(R.id.gateway_client_username);
            connectionStatus = itemView.findViewById(R.id.gateway_client_connection_status);
        }
    }
}
