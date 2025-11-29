package dev.cazimir.floatnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.cazimir.floatnote.data.HistoryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HistoryViewModel(private val historyManager: HistoryManager) : ViewModel() {
    val historyFlow: Flow<List<String>> = historyManager.historyFlow

    fun addEntry(text: String) {
        viewModelScope.launch { historyManager.addEntry(text) }
    }

    fun clearAll() {
        viewModelScope.launch { historyManager.clearAll() }
    }
}

