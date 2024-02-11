package com.afkanerd.deku.QueueListener.GatewayClients;

import static com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient.DIFF_CALLBACK;

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
        holder.projectNameTextView.setText(mDiffer.getCurrentList().get(position).name);
        holder.projectBinding1TextView.setText(mDiffer.getCurrentList().get(position).binding1Name);
        holder.projectBinding2TextView.setText(mDiffer.getCurrentList().get(position).binding2Name);
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

            cardView = itemView.findViewById(R.id.gateway_client_card);
            projectNameTextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_name);

            projectBinding1TextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_binding1);

            projectBinding2TextView =
                    itemView.findViewById(R.id.gateway_client_project_listing_project_binding2);
        }
    }
}
