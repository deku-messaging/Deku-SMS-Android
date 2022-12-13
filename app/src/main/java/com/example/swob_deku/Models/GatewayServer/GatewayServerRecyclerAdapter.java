package com.example.swob_deku.Models.GatewayServer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GatewayServerRecyclerAdapter extends RecyclerView.Adapter<GatewayServerRecyclerAdapter.ViewHolder> {

    int recentsRenderLayout;
    Context context;
    List<GatewayServer> gatewayServerList;

    public GatewayServerRecyclerAdapter(Context context, List<GatewayServer> gatewayServerList, int recentsRenderLayout) {
        this.context = context;
        this.gatewayServerList = gatewayServerList;
        this.recentsRenderLayout = recentsRenderLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(this.recentsRenderLayout, parent, false);
        return new GatewayServerRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GatewayServer gatewayServer = gatewayServerList.get(position);
        holder.url.setText(gatewayServer.getURL());
        holder.method.setText(gatewayServer.getMethod());
        holder.date.setText(Long.toString(gatewayServer.getDate()));
    }

    @Override
    public int getItemCount() {
        return this.gatewayServerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView url;
        TextView method;
        TextView date;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            this.url = itemView.findViewById(R.id.gateway_server_url);
            this.method = itemView.findViewById(R.id.gateway_server_method);
            this.date = itemView.findViewById(R.id.gateway_server_date);
        }
    }
}
