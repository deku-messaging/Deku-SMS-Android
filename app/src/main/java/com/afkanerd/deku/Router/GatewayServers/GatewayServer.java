package com.afkanerd.deku.Router.GatewayServers;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.afkanerd.deku.DefaultSMS.Models.Database.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Database.Migrations;

@Entity
public class GatewayServer {

    public static String BASE64_FORMAT = "base_64";
    public static String ALL_FORMAT = "all";
    public static String POST_PROTOCOL = "POST";
    public static String GET_PROTOCOL = "GET";

    public static String GATEWAY_SERVER_ID = "GATEWAY_SERVER_ID";
    public static String GATEWAY_SERVER_TAG = "GATEWAY_SERVER_TAG";
    public static String GATEWAY_SERVER_URL = "GATEWAY_SERVER_URL";
    public static String GATEWAY_SERVER_PROTOCOL = "GATEWAY_SERVER_PROTOCOL";
    public static String GATEWAY_SERVER_FORMAT = "GATEWAY_SERVER_FORMAT";

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

    @Ignore
    Datastore databaseConnector;
    public GatewayServerDAO getDaoInstance(Context context) {
        databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .enableMultiInstanceInvalidation()
                .addMigrations(new Migrations.Migration8To9())
                .build();
        return databaseConnector.gatewayServerDAO();
    }

    public void close() {
        if(databaseConnector != null)
            databaseConnector.close();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
//        return super.equals(obj);
        if(obj instanceof GatewayServer) {
            GatewayServer gatewayServer = (GatewayServer) obj;
            return gatewayServer.id == this.id &&
                    gatewayServer.URL.equals(this.URL) &&
                    gatewayServer.protocol.equals(this.protocol) &&
                    gatewayServer.date.equals(this.date);
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
}
