package dev.cazimir.floatnote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore by preferencesDataStore(name = "history")

class HistoryManager(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("history_entries")
        private const val SEPARATOR = "\n" // newline separated
        private const val MAX_ENTRIES = 200 // simple cap to avoid unbounded growth
    }

    val historyFlow: Flow<List<String>> = context.historyDataStore.data.map { prefs ->
        prefs[KEY_HISTORY]
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun addEntry(text: String) {
        if (text.isBlank()) return
        context.historyDataStore.edit { prefs ->
            val existing = prefs[KEY_HISTORY]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val newList = (existing + text).takeLast(MAX_ENTRIES)
            prefs[KEY_HISTORY] = newList.joinToString(SEPARATOR)
        }
    }

    suspend fun clearAll() {
        context.historyDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }
}
