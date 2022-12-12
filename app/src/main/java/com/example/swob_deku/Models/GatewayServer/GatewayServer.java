package com.example.swob_deku.Models.GatewayServer;

import androidx.room.Entity;

@Entity
public class GatewayServer {
    String URL;
    String method = "POST";
}
