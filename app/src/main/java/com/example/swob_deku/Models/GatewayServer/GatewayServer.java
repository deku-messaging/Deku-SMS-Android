package com.example.swob_deku.Models.GatewayServer;

import androidx.room.Entity;

@Entity
public class GatewayServer {
    String URL;

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

    String method = "POST";

    Long date;

    public GatewayServer(String url) {
        this.URL = url;
    }
}
