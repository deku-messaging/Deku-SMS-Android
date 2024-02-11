package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;

public class GatewayClientProjects {

    public String name;
    public String binding1Name;
    public String binding2Name;


    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof GatewayClientProjects) {
            GatewayClientProjects gatewayClientProjects = (GatewayClientProjects) obj;
            return Objects.equals(gatewayClientProjects.name, this.name) &&
                    Objects.equals(gatewayClientProjects.binding1Name, this.binding1Name) &&
                    Objects.equals(gatewayClientProjects.binding2Name, this.binding2Name);
        }
        return false;
    }

    public static final DiffUtil.ItemCallback<GatewayClientProjects> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GatewayClientProjects>() {
                @Override
                public boolean areItemsTheSame(@NonNull GatewayClientProjects oldItem,
                                               @NonNull GatewayClientProjects newItem) {
                    return oldItem.name.equals(newItem.name);
                }

                @Override
                public boolean areContentsTheSame(@NonNull GatewayClientProjects oldItem,
                                                  @NonNull GatewayClientProjects newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
