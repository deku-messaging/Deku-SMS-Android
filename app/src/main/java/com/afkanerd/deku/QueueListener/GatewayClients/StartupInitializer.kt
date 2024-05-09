package com.afkanerd.deku.QueueListener.GatewayClients

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.startup.Initializer

class StartupInitializer : Initializer<AppCompatActivity> {
    override fun create(context: Context): AppCompatActivity {
        TODO("Not yet implemented")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(StartupInitializer::class.java)
    }
}