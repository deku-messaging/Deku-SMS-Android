package com.afkanerd.deku.Forwarder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gatewayclient_settings")

class GatewayClientsSettingsManager(private val context: Context) {

    private val prefRouteOnLowBattery = "prefRouteOnLowBattery"
    private val prefClearRouteCache = "prefClearRouteCache"
    private val prefHashIncomingAddress = "prefHashIncomingAddress"

    suspend fun setRouteOnLowBattery(routeOnLowBattery: Boolean) {
        context.dataStore.updateData {
            val key = booleanPreferencesKey(prefRouteOnLowBattery)
            it.toMutablePreferences().also { preferences ->
                preferences[key] = routeOnLowBattery
            }
        }
    }

    fun getRouteOnLowBattery(): Flow<Boolean>  =
        context.dataStore.data.map { preferences ->
            val key = booleanPreferencesKey(prefRouteOnLowBattery)
            preferences[key] ?: true
        }

    suspend fun setClearRouteCache(clear: Boolean) {
        context.dataStore.updateData {
            val key = booleanPreferencesKey(prefClearRouteCache)
            it.toMutablePreferences().also { preferences ->
                preferences[key] = clear
            }
        }
    }

    fun getClearRouteCache(): Flow<Boolean>  =
        context.dataStore.data.map { preferences ->
            val key = booleanPreferencesKey(prefClearRouteCache)
            preferences[key] ?: false
        }


    suspend fun setHashIncomingAddress(clear: Boolean) {
        context.dataStore.updateData {
            val key = booleanPreferencesKey(prefHashIncomingAddress)
            it.toMutablePreferences().also { preferences ->
                preferences[key] = clear
            }
        }
    }

    fun getHashIncomingAddress(): Flow<Boolean>  =
        context.dataStore.data.map { preferences ->
            val key = booleanPreferencesKey(prefHashIncomingAddress)
            preferences[key] ?: false
        }
}