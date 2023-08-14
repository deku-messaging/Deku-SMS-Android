package com.example.swob_deku.Models.GatewayServers;

import static com.example.swob_deku.Models.GatewayServers.GatewayServer.DIFF_CALLBACK;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.GatewayServerAddActivity;
import com.example.swob_deku.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GatewayServerRecyclerAdapter extends RecyclerView.Adapter<GatewayServerRecyclerAdapter.ViewHolder> {

    private final AsyncListDiffer<GatewayServer> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);
    Context context;

    public GatewayServerRecyclerAdapter(Context context) {
        this.context = context;
    }

    public GatewayServerRecyclerAdapter() {}

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.gateway_server_listing_layout, parent, false);
        return new GatewayServerRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        GatewayServer gatewayServer = gatewayServerList.get(position);
        GatewayServer gatewayServer = mDiffer.getCurrentList().get(position);
        holder.url.setText(gatewayServer.getURL());
        holder.protocol.setText(gatewayServer.getProtocol());

        String dataFormat = gatewayServer.getFormat().isEmpty() ? "All" : gatewayServer.getFormat();
        holder.format.setText(dataFormat);

        String date = Helpers.formatDate(context, gatewayServer.getDate());
        holder.date.setText(date);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GatewayServerAddActivity.class);
                intent.putExtra(GatewayServer.GATEWAY_SERVER_ID, gatewayServer.getId());
                intent.putExtra(GatewayServer.GATEWAY_SERVER_TAG, gatewayServer.getTag());
                intent.putExtra(GatewayServer.GATEWAY_SERVER_URL, gatewayServer.getURL());
                intent.putExtra(GatewayServer.GATEWAY_SERVER_PROTOCOL, gatewayServer.getProtocol());
                intent.putExtra(GatewayServer.GATEWAY_SERVER_FORMAT, gatewayServer.getFormat());
                context.startActivity(intent);
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
