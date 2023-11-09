package com.afkanerd.deku.Router.Router;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouterRecyclerAdapter extends RecyclerView.Adapter<RouterRecyclerAdapter.ViewHolder> {
    public final AsyncListDiffer<RouterItem> mDiffer = new AsyncListDiffer<>(this, RouterItem.DIFF_CALLBACK);

    Context context;

    public MutableLiveData<Set<Long>> selectedItems;
    public RouterRecyclerAdapter(Context context) {
        this.context = context;
        this.selectedItems = new MutableLiveData<>();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(R.layout.routed_messages_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouterItem routerItem = mDiffer.getCurrentList().get(position);
        holder.bind(routerItem);
        setOnLongClickListener(holder);
        setOnClickListener(holder);
    }
    private void setOnClickListener(ViewHolder holder) {
       holder.materialCardView.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Set<Long> items = selectedItems.getValue();
               if(items != null && !items.isEmpty()){
                   if(items.contains(holder.getItemId())) {
                       items.remove(holder.getItemId());
                       holder.unhighlight();
                   } else {
                       Log.d(getClass().getName(), "Item id: " + holder.getItemId());
                       items.add(holder.getItemId());
                       holder.highlight();
                   }
                   selectedItems.setValue(items);
               }
           }
       });
    }

    private void setOnLongClickListener(ViewHolder holder) {
        holder.materialCardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Set<Long> items = selectedItems.getValue();
                if(items == null)
                    items = new HashSet<>();
                Log.d(getClass().getName(), "Item id: " + holder.getItemId());
                items.add(holder.getItemId());
                selectedItems.setValue(items);
                holder.highlight();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<RouterItem> list) {
        mDiffer.submitList(list);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView materialCardView;
        TextView address, url, body, status, date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.address = itemView.findViewById(R.id.routed_messages_address);
            this.url = itemView.findViewById(R.id.routed_messages_url);
            this.body = itemView.findViewById(R.id.routed_messages_body);
            this.status = itemView.findViewById(R.id.routed_messages_status);
            this.date = itemView.findViewById(R.id.routed_messages_date);
            this.materialCardView = itemView.findViewById(R.id.routed_messages_material_cardview);
        }

        public void bind(RouterItem routerItem) {
            this.address.setText(routerItem.getAddress());
            this.url.setText(routerItem.url);
            this.body.setText(routerItem.getBody());
            this.status.setText(routerItem.routingStatus);
            this.date.setText(Helpers.formatDate(itemView.getContext(),
                    routerItem.routingDate));
        }

        public void highlight() {
            this.materialCardView.setBackgroundResource(R.drawable.received_messages_drawable);
        }
        public void unhighlight() {
            this.materialCardView.setBackgroundResource(0);
        }
    }
}
