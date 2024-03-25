package com.afkanerd.deku.Router.GatewayServers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Fragments.ModalSheetFragment;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.SMTP;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GatewayServerRecyclerAdapter extends RecyclerView.Adapter<GatewayServerRecyclerAdapter.ViewHolder> {

    private final AsyncListDiffer<GatewayServer> mDiffer = new AsyncListDiffer(this, GatewayServer.DIFF_CALLBACK);

    public GatewayServerRecyclerAdapter() { }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.gateway_server_listing_layout, parent, false);
        return new GatewayServerRecyclerAdapter.ViewHolder(view);
    }

    public MutableLiveData<GatewayServer> gatewayServerClickedListener = new MutableLiveData<>();
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        GatewayServer gatewayServer = gatewayServerList.get(position);
        GatewayServer gatewayServer = mDiffer.getCurrentList().get(position);
        String url = gatewayServer.getProtocol().equals(SMTP.PROTOCOL) ?
                gatewayServer.smtp.host : gatewayServer.getURL();
        holder.url.setText(url);
        holder.protocol.setText(gatewayServer.getProtocol().equals("POST") ? "HTTPS" :
                gatewayServer.getProtocol());

        String dataFormat = (gatewayServer.getFormat() == null || gatewayServer.getFormat().isEmpty())
                ? "all" : gatewayServer.getFormat();
        holder.format.setText(dataFormat);

        String date = Helpers.formatDate(holder.itemView.getContext(), gatewayServer.getDate());
        holder.date.setText(date);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gatewayServerClickedListener.setValue(gatewayServer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<GatewayServer> list) {
        mDiffer.submitList(list);
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, format, protocol, url;

        CardView cardView;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            this.url = itemView.findViewById(R.id.gateway_server_url);
            this.protocol = itemView.findViewById(R.id.gateway_server_protocol);
            this.date = itemView.findViewById(R.id.gateway_server_date);
            this.format = itemView.findViewById(R.id.gateway_server_data_format);
            this.cardView = itemView.findViewById(R.id.gateway_server_card_view);
        }
    }
}
