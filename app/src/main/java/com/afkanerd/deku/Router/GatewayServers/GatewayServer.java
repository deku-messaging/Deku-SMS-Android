package com.afkanerd.deku.Router.GatewayServers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.Router.FTP;
import com.afkanerd.deku.Router.Models.RouterHandler;
import com.afkanerd.deku.Router.Models.RouterWorkManager;
import com.afkanerd.deku.Router.SMTP;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Entity
public class GatewayServer {
    @Embedded public SMTP smtp = new SMTP();

    @Embedded public FTP ftp = new FTP();

    public static String BASE64_FORMAT = "base_64";
    public static String ALL_FORMAT = "all";
    public static String POST_PROTOCOL = "HTTPS";
    public static String GATEWAY_SERVER_ID = "GATEWAY_SERVER_ID";

    @ColumnInfo(name="URL")
    public String URL;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    @ColumnInfo(name="protocol")
    public String protocol = POST_PROTOCOL;

    @ColumnInfo(name="tag")
    public String tag = "";

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @ColumnInfo(name="format")
    public String format = ALL_FORMAT;

    @ColumnInfo(name="date")
    public Long date;

    public GatewayServer(String url) {
        this.URL = url;
    }

    public GatewayServer() {}

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long getId(){
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
//        return super.equals(obj);
        if(obj instanceof GatewayServer) {
            GatewayServer gatewayServer = (GatewayServer) obj;
            return gatewayServer.id == this.id &&
                    Objects.equals(gatewayServer.URL, this.URL) &&
                    Objects.equals(gatewayServer.protocol, this.protocol) &&
                    Objects.equals(gatewayServer.date, this.date) &&
                    Objects.equals(gatewayServer.smtp, this.smtp);
        }
        return false;
    }

    public static final DiffUtil.ItemCallback<GatewayServer> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GatewayServer>() {
                @Override
                public boolean areItemsTheSame(@NonNull GatewayServer oldItem, @NonNull GatewayServer newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull GatewayServer oldItem, @NonNull GatewayServer newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public static void route(Context context, final Conversation conversation) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        boolean isBase64 = Helpers.isBase64Encoded(conversation.getText());
        List<GatewayServer> gatewayServerList =
                Datastore.getDatastore(context.getApplicationContext())
                        .gatewayServerDAO()
                        .getAllList();

        for (GatewayServer gatewayServer1 : gatewayServerList) {
            if(gatewayServer1.getFormat() != null &&
                    gatewayServer1.getFormat().equals(GatewayServer.BASE64_FORMAT) && !isBase64)
                continue;

            try {
                OneTimeWorkRequest routeMessageWorkRequest =
                        new OneTimeWorkRequest.Builder(RouterWorkManager.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(RouterHandler.TAG_NAME_GATEWAY_SERVER)
                                .addTag(RouterHandler.INSTANCE
                                        .getTagForMessages(conversation.getMessage_id()))
                                .addTag(RouterHandler.INSTANCE
                                        .getTagForGatewayServers(gatewayServer1.getId()))
                                .setInputData(new Data.Builder()
                                        .putLong(RouterWorkManager.Companion.getGATEWAY_SERVER_ID(),
                                                gatewayServer1.getId())
                                        .putString(RouterWorkManager.Companion.getCONVERSATION_ID(),
                                                conversation.getMessage_id())
                                        .build())
                                .build();

                String uniqueWorkName = conversation.getMessage_id() + ":" +
                        gatewayServer1.getURL() + ":" + gatewayServer1.getProtocol();
                WorkManager workManager = WorkManager.getInstance(context);
                Operation operation = workManager.enqueueUniqueWork(
                        uniqueWorkName,
                        ExistingWorkPolicy.KEEP,
                        routeMessageWorkRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
