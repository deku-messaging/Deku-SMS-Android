package com.afkanerd.deku.Router.GatewayServers;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.Models.RouterHandler;
import com.afkanerd.deku.Router.Models.RouterItem;
import com.afkanerd.deku.Router.SMTP;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatewayServerRouterRecyclerAdapter extends RecyclerView.Adapter<GatewayServerRouterRecyclerAdapter.ViewHolder> {
    private final AsyncListDiffer<Pair<RouterItem, GatewayServer>> mDiffer =
            new AsyncListDiffer<>(this, RouterItem.DIFF_CALLBACK);

    public void submitList(Context context, final List<WorkInfo> workInfoList) throws InterruptedException {
        List<Pair<RouterItem, GatewayServer>> workInfoItemsList = new ArrayList<>();
        Map<Long, Pair<RouterItem, GatewayServer>> map = new HashMap<>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(WorkInfo workInfo : workInfoList) {
                    Pair<String, String> workInfoPair = RouterHandler.workInfoParser(workInfo);

                    GatewayServer gatewayServer = Datastore.getDatastore(context)
                            .gatewayServerDAO().get(workInfoPair.second);
                    if(gatewayServer == null)
                        continue;

                    RouterItem routerItem = new RouterItem(Datastore.getDatastore(context)
                            .conversationDao().getMessage(workInfoPair.first));
                    routerItem.routingUniqueId = workInfo.getId().toString();
                    routerItem.routingStatus = RouterHandler.reverseState(context,
                            workInfo.getState());
                    map.put(Long.parseLong(routerItem.getDate()),
                            new Pair<>(routerItem, gatewayServer));
                }
            }
        });

        thread.start();
        thread.join();

        mDiffer.submitList(new ArrayList<>(map.values()));
    }

    public MutableLiveData<HashMap<Long, ViewHolder>> selectedItems;
    public GatewayServerRouterRecyclerAdapter() {
        this.selectedItems = new MutableLiveData<>();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.gateway_server_routed_messages_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            holder.bind(mDiffer.getCurrentList().get(position));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setOnClickListener(ViewHolder holder) {
       holder.materialCardView.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               HashMap<Long, ViewHolder> items = selectedItems.getValue();
               if(items != null && !items.isEmpty()){
                   if(items.containsKey(holder.getItemId())) {
                       ViewHolder viewHolder = items.remove(holder.getItemId());
                       viewHolder.unhighlight();
                   } else {
                       Log.d(getClass().getName(), "Item id: " + holder.getItemId());
                       items.put(holder.getItemId(), holder);
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
                HashMap<Long, ViewHolder> items = selectedItems.getValue();
                if(items == null)
                    items = new HashMap<>();
                Log.d(getClass().getName(), "Item id: " + holder.getItemId());
                items.put(holder.getItemId(), holder);
                selectedItems.setValue(items);
                holder.highlight();
                return true;
            }
        });
    }

    public void resetAllSelected() {
        for(Map.Entry<Long, ViewHolder> entry : selectedItems.getValue().entrySet()) {
            entry.getValue().unhighlight();
        }
        selectedItems.setValue(null);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
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

        public void bind(Pair<RouterItem, GatewayServer> conversationGatewayServerPair)
                throws InterruptedException {
            RouterItem conversation = conversationGatewayServerPair.first;
            GatewayServer gatewayServer = conversationGatewayServerPair.second;

            String gatewayServerUrl = gatewayServer.getProtocol().equals(SMTP.PROTOCOL) ?
                    gatewayServer.smtp.host :
                    gatewayServer.getURL();

            String protAddress = gatewayServer.getProtocol() + "/" + conversation.getAddress();
            address.setText(protAddress);
            url.setText(gatewayServerUrl);
            body.setText(conversation.getText());
            status.setText(conversation.routingStatus);
            date.setText(Helpers.formatDate(itemView.getContext(),
                    Long.parseLong(conversation.getDate())));

        }

        public void highlight() {
            this.materialCardView.setBackgroundResource(R.drawable.received_messages_drawable);
        }
        public void unhighlight() {
            this.materialCardView.setBackgroundResource(0);
        }
    }

}
