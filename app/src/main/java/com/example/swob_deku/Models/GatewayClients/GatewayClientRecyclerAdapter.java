package com.example.swob_deku.Models.GatewayClients;

import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_FRIENDLY_NAME;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_HOST;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_ID;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_PASSWORD;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_PORT;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_STOP_LISTENERS;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_USERNAME;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_VIRTUAL_HOST;
import static com.example.swob_deku.Models.GatewayClients.GatewayClient.DIFF_CALLBACK;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.R;
import com.example.swob_deku.Services.RMQConnectionService;
import com.example.swob_deku.Services.ServiceHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;

public class GatewayClientRecyclerAdapter extends RecyclerView.Adapter<GatewayClientRecyclerAdapter.ViewHolder>{
    private final AsyncListDiffer<GatewayClient> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    List<ActivityManager.RunningServiceInfo> runningServiceInfoList = new ArrayList<>();

    Context context;

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

        String date = Helpers.formatDate(context, gatewayClient.getDate());
        holder.date.setText(date);

        if(gatewayClient.getFriendlyConnectionName() == null ||
                gatewayClient.getFriendlyConnectionName().isEmpty())
            holder.friendlyName.setVisibility(View.GONE);
        else
            holder.friendlyName.setText(gatewayClient.getFriendlyConnectionName());

        holder.listeningSwitch.setChecked(sharedPreferences.contains(String.valueOf(gatewayClient.getId())));

        holder.listeningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !sharedPreferences.contains(String.valueOf(gatewayClient.getId()))) {
                    startListening(gatewayClient);
                } else if(!isChecked && sharedPreferences.contains(String.valueOf(gatewayClient.getId()))) {
                    stopListening(gatewayClient);
                }
            }
        });
    }

    public void rmqConnectionStateChanged() {
        runningServiceInfoList = ServiceHandler.getRunningService(context);
        notifyDataSetChanged();
    }

    public void startListening(GatewayClient gatewayClient) {
        Intent intent = new Intent(context, RMQConnectionService.class);
        intent.putExtra(GATEWAY_CLIENT_ID, gatewayClient.getId());
        intent.putExtra(GATEWAY_CLIENT_USERNAME, gatewayClient.getUsername());
        intent.putExtra(GATEWAY_CLIENT_PASSWORD, gatewayClient.getPassword());
        intent.putExtra(GATEWAY_CLIENT_HOST, gatewayClient.getHostUrl());
        intent.putExtra(GATEWAY_CLIENT_PORT, gatewayClient.getPort());
        intent.putExtra(GATEWAY_CLIENT_VIRTUAL_HOST, gatewayClient.getVirtualHost());
        intent.putExtra(GATEWAY_CLIENT_FRIENDLY_NAME, gatewayClient.getFriendlyConnectionName());
        context.startService(intent);
    }

    public void stopListening(GatewayClient gatewayClient) {
        Intent intent = new Intent(context, RMQConnectionService.class);
        intent.putExtra(GATEWAY_CLIENT_ID, gatewayClient.getId());
        intent.putExtra(GATEWAY_CLIENT_STOP_LISTENERS, true);
        context.startService(intent);
    }

    public void submitList(List<GatewayClient> gatewayClientList) {
        mDiffer.submitList(gatewayClientList);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView url, virtualHost, friendlyName, date;

        SwitchCompat listeningSwitch;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            url = itemView.findViewById(R.id.gateway_client_url);
            virtualHost = itemView.findViewById(R.id.gateway_client_virtual_host);
            friendlyName = itemView.findViewById(R.id.gateway_client_friendly_name_text);
            date = itemView.findViewById(R.id.gateway_client_date);
            listeningSwitch = itemView.findViewById(R.id.gateway_client_start_listening_switch);
        }
    }
}
