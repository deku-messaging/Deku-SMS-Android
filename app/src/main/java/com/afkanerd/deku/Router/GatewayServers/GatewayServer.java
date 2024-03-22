package com.afkanerd.deku.Router.GatewayServers;

import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_NAME;
import static com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver.TAG_ROUTING_URL;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;
import com.afkanerd.deku.Router.Router.RouterHandler;
import com.afkanerd.deku.Router.Router.RouterItem;
import com.afkanerd.deku.Router.Router.RouterWorkManager;
import com.afkanerd.deku.Router.SMTP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Entity
public class GatewayServer {
    @Embedded public SMTP smtp = new SMTP();
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
                    gatewayServer.URL.equals(this.URL) &&
                    gatewayServer.protocol.equals(this.protocol) &&
                    gatewayServer.date.equals(this.date) &&
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

    public static void route(Context context, Conversation conversation) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting().serializeNulls();
        Gson gson = gsonBuilder.create();

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

            RouterItem routerItem = (RouterItem) conversation;
            routerItem.MSISDN = conversation.getAddress();
            routerItem.text = conversation.getText();
            routerItem.tag = gatewayServer1.getTag();
            final String jsonStringBody = gson.toJson(routerItem);

            try {
                OneTimeWorkRequest routeMessageWorkRequest =
                        new OneTimeWorkRequest.Builder(RouterWorkManager.class)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS
                                )
                                .addTag(TAG_NAME)
                                .addTag(RouterHandler.getTagForMessages(routerItem.getMessage_id()))
                                .addTag(RouterHandler.getTagForGatewayServers(gatewayServer1.getURL()))
                                .setInputData(
                                        new Data.Builder()
                                                .putString(RouterWorkManager.SMS_JSON_OBJECT,
                                                        jsonStringBody)
                                                .putString(RouterWorkManager.SMS_JSON_ROUTING_URL,
                                                        gatewayServer1.getURL())
                                                .build()
                                ).build();

                String uniqueWorkName = routerItem.getMessage_id() + ":" + gatewayServer1.getURL();
                WorkManager workManager = WorkManager.getInstance(context);
                workManager.enqueueUniqueWork(
                        uniqueWorkName,
                        ExistingWorkPolicy.KEEP,
                        routeMessageWorkRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
