package com.example.swob_deku.Models.GatewayServer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value={"URL"}, unique = true)})
public class GatewayServer {
    @ColumnInfo(name="URL")
    public String URL;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    @ColumnInfo(name="method")
    public String method = "POST";

    @ColumnInfo(name="date")
    public Long date;

    public GatewayServer(String url) {
        this.URL = url;
    }

    public GatewayServer() {}

    @PrimaryKey(autoGenerate = true)
    public long id;
}
