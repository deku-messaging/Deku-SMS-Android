package com.example.swob_deku.Models.GatewayClients;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.swob_deku.R;

@Entity(indices = {@Index(value={"hostUrl"}, unique = true)})
public class GatewayClient {
    public GatewayClient() {}

    @PrimaryKey(autoGenerate = true)
    int id;


    long date;

    String hostUrl;

    String username;
    String password;

    int port;

    String friendlyConnectionName;
    String virtualHost;

    int connectionTimeout = 10000;

    int prefetch_count = 1;

    int heartbeat = 30;

    String protocol = "amqp";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFriendlyConnectionName() {
        return friendlyConnectionName;
    }

    public void setFriendlyConnectionName(String friendlyConnectionName) {
        this.friendlyConnectionName = friendlyConnectionName;
    }

    public int getPrefetch_count() {
        return prefetch_count;
    }

    public void setPrefetch_count(int prefetch_count) {
        this.prefetch_count = prefetch_count;
    }

    public int getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }
    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    public boolean equals(@Nullable Object obj) {
//        return super.equals(obj);
        if(obj instanceof GatewayClient) {
            GatewayClient gatewayServer = (GatewayClient) obj;
            return gatewayServer.id == this.id &&
                    gatewayServer.hostUrl.equals(this.hostUrl) &&
                    gatewayServer.protocol.equals(this.protocol) &&
                    gatewayServer.port == this.port &&
                    gatewayServer.date == this.date;
        }
        return false;
    }
    public static final DiffUtil.ItemCallback<GatewayClient> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GatewayClient>() {
                @Override
                public boolean areItemsTheSame(@NonNull GatewayClient oldItem, @NonNull GatewayClient newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull GatewayClient oldItem, @NonNull GatewayClient newItem) {
                    return oldItem.equals(newItem);
                }
            };

}
