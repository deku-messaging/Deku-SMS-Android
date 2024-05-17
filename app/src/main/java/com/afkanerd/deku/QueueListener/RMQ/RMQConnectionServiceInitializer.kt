package com.afkanerd.deku.QueueListener.RMQ

import android.content.Context
import android.content.Intent
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import com.afkanerd.deku.NotificationsInitializer

class RMQConnectionServiceInitializer : Initializer<Intent> {
    override fun create(context: Context): Intent {
        val intent = Intent(context, RMQConnectionService::class.java)

        context.startForegroundService(intent)

        return intent
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java,
                NotificationsInitializer::class.java)
    }
}