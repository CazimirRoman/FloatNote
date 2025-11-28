package dev.cazimir.floatnote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private val cryptoManager = CryptoManager()

    val apiKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            val encryptedKey = preferences[KEY_API_KEY] ?: ""
            if (encryptedKey.isNotEmpty()) {
                cryptoManager.decrypt(encryptedKey)
            } else {
                ""
            }
        }

    suspend fun saveApiKey(apiKey: String) {
        val encryptedKey = cryptoManager.encrypt(apiKey)
        context.dataStore.edit { preferences ->
            preferences[KEY_API_KEY] = encryptedKey
        }
    }

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
    }
}
