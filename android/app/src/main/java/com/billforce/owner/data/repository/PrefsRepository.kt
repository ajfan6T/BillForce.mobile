package com.billforce.owner.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.billforce.owner.data.model.DbConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "billforce_prefs")

/**
 * Persists connection preferences (DB path, PIN, shop name) using DataStore.
 */
class PrefsRepository(private val context: Context) {

    companion object {
        private val KEY_DB_PATH           = stringPreferencesKey("db_path")
        private val KEY_SHOP_NAME         = stringPreferencesKey("shop_name")
        private val KEY_OWNER_PIN         = stringPreferencesKey("owner_pin")
        private val KEY_BIOMETRIC         = booleanPreferencesKey("biometric_enabled")
        private val KEY_IS_CONNECTED      = booleanPreferencesKey("is_connected")
        private val KEY_LAST_CONNECTED    = longPreferencesKey("last_connected_at")
    }

    val dbConfigFlow: Flow<DbConfig> = context.dataStore.data.map { prefs ->
        DbConfig(
            dbFilePath          = prefs[KEY_DB_PATH] ?: "",
            shopName            = prefs[KEY_SHOP_NAME] ?: "",
            ownerPin            = prefs[KEY_OWNER_PIN] ?: "",
            isBiometricEnabled  = prefs[KEY_BIOMETRIC] ?: false,
            isConnected         = prefs[KEY_IS_CONNECTED] ?: false,
            lastConnectedAt     = prefs[KEY_LAST_CONNECTED] ?: 0L
        )
    }

    suspend fun saveDbConfig(config: DbConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DB_PATH]        = config.dbFilePath
            prefs[KEY_SHOP_NAME]      = config.shopName
            prefs[KEY_OWNER_PIN]      = config.ownerPin
            prefs[KEY_BIOMETRIC]      = config.isBiometricEnabled
            prefs[KEY_IS_CONNECTED]   = config.isConnected
            prefs[KEY_LAST_CONNECTED] = config.lastConnectedAt
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit { prefs ->
            prefs[KEY_DB_PATH]      = ""
            prefs[KEY_IS_CONNECTED] = false
        }
    }
}
