package com.afkanerd.deku.QueueListener.GatewayClients;

import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient.DIFF_CALLBACK;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.R;

import org.jetbrains.annotations.NotNull;

public class GatewayClientProjectListingRecyclerAdapter extends RecyclerView.Adapter<GatewayClientProjectListingRecyclerAdapter.ViewHolder>{
    public final AsyncListDiffer<GatewayClientProjects> mDiffer =
            new AsyncListDiffer<>(this, GatewayClientProjects.DIFF_CALLBACK);
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.gateway_client_project_listing_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GatewayClientProjects gatewayClientProjects = mDiffer.getCurrentList().get(position);
        holder.projectNameTextView.setText(gatewayClientProjects.name);
        holder.projectBinding1TextView.setText(gatewayClientProjects.binding1Name);
        holder.projectBinding2TextView.setText(gatewayClientProjects.binding2Name);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.itemView.getContext(), GatewayClientCustomizationActivity.class);
                intent.putExtra(GatewayClientListingActivity.GATEWAY_CLIENT_ID, gatewayClientProjects.gatewayClientId);
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView projectNameTextView;
        TextView projectBinding1TextView, projectBinding2TextView;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.gateway_client_project_listing_card );
            projectNameTextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_name);

            projectBinding1TextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_binding1);

            projectBinding2TextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_binding2);
        }
    }
}
