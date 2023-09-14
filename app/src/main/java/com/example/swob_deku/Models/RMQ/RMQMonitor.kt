package com.example.swob_deku.Models.RMQ

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.swob_deku.GatewayClientListingActivity
import kotlin.concurrent.thread

class RMQMonitor(val context: Context, private val gatewayClientId: Long,
                 private val rmqConnection: RMQConnection) {

    private val sharedPreferences : SharedPreferences = context.getSharedPreferences(
            GatewayClientListingActivity.GATEWAY_CLIENT_LISTENERS, 0)

    private val activeThreads : HashMap<String, Thread> = HashMap();

    private fun setMonitorTimeout(timeout : Long) {
        /**
         * This should monitor if the connection is still open.
         * If the connection is not open, this  should modify something which
         *  informs the service that it is trying to reconnect
         */
        val activeThread = Thread(Runnable {
            while(true) {
                Log.d(javaClass.simpleName, "Kt Attempting a reconnect..");
                if (rmqConnection.connection.isOpen) {
                    sharedPreferences.edit()
                            .putBoolean(this.gatewayClientId.toString(), true)
                            .apply();
                    break
                } else {
                    Thread.sleep(timeout)
                }
            }
            activeThreads.remove(this.gatewayClientId.toString())
        })
        activeThread.start();
        activeThreads[gatewayClientId.toString()] = activeThread
    }

    fun setConnected(delayTimeout : Long ) {
        sharedPreferences.edit()
                .putBoolean(this.gatewayClientId.toString(), (delayTimeout == 0L))
                .apply();

        if(delayTimeout > 0 && !activeThreads.containsKey(gatewayClientId.toString()) &&
                rmqConnection.connection != null)
            setMonitorTimeout(delayTimeout)
    }

    fun getRmqConnection() : RMQConnection {
        return this.rmqConnection
    }
}