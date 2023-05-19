package com.example.swob_deku.Models.GatewayServer;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value={"URL"}, unique = true)})
public class GatewayServer {

    public static String BASE64_FORMAT = "base_64";
    public static String ALL_FORMAT = "all";
    public static String POST_PROTOCOL = "POST";
    public static String GET_PROTOCOL = "GET";

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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @ColumnInfo(name="format")
    public String format;

    @ColumnInfo(name="date")
    public Long date;

    public GatewayServer(String url) {
        this.URL = url;
    }

    public GatewayServer() {}

    @PrimaryKey(autoGenerate = true)
    public long id;

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
}
