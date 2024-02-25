package com.afkanerd.deku.QueueListener.GatewayClients;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.afkanerd.deku.DefaultSMS.R;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Entity
public class GatewayClient {
    public GatewayClient() {}

    @Ignore
    private String connectionStatus;

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @PrimaryKey(autoGenerate = true)
    private long id;


    private long date;

    private String hostUrl;

    private String username;
    private String password;

    private int port;

    private String friendlyConnectionName;
    private String virtualHost;

    private int connectionTimeout = 10000;

    private int prefetch_count = 1;

    private int heartbeat = 30;

    private String protocol = "amqp";

    private String projectName;

    private String projectBinding;

    public String getProjectBinding2() {
        return projectBinding2;
    }

    public void setProjectBinding2(String projectBinding2) {
        this.projectBinding2 = projectBinding2;
    }

    private String projectBinding2;

    public String getProjectName() {
        return projectName;
    }

    public String getProjectBinding() {
        return projectBinding;
    }

    public void setProjectBinding(String projectBinding) {
        this.projectBinding = projectBinding;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


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


    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public long[] getHashcode() {
        String hashValues = protocol + hostUrl + port + virtualHost + username + password;
        return MurmurHash3.hash128(hashValues.getBytes(StandardCharsets.UTF_8));
    }

    public boolean same(@Nullable Object obj) {
        if(obj instanceof GatewayClient) {
            GatewayClient gatewayClient = (GatewayClient) obj;
            return Objects.equals(gatewayClient.hostUrl, this.hostUrl) &&
                    Objects.equals(gatewayClient.protocol, this.protocol) &&
                    Objects.equals(gatewayClient.virtualHost, this.virtualHost) &&
                    gatewayClient.port == this.port;
        }
        return false;
    }

    public boolean equals(@Nullable Object obj) {
//        return super.equals(obj);
        if(obj instanceof GatewayClient) {
            GatewayClient gatewayClient = (GatewayClient) obj;
//            return gatewayClient.id == this.id &&
//                    Objects.equals(gatewayClient.hostUrl, this.hostUrl) &&
//                    Objects.equals(gatewayClient.protocol, this.protocol) &&
//                    gatewayClient.port == this.port &&
//                    Objects.equals(gatewayClient.projectBinding, this.projectBinding) &&
//                    Objects.equals(gatewayClient.projectName, this.projectName) &&
//                    Objects.equals(gatewayClient.connectionStatus, this.connectionStatus) &&
//                    gatewayClient.date == this.date;
            return Objects.equals(gatewayClient.hostUrl, this.hostUrl) &&
                    Objects.equals(gatewayClient.protocol, this.protocol) &&
                    Objects.equals(gatewayClient.virtualHost, this.virtualHost) &&
                    gatewayClient.port == this.port;
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
