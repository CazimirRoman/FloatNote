package dev.cazimir.floatnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    val apiKeyFlow: Flow<String> = settingsManager.apiKeyFlow
    val languageFlow: Flow<String> = settingsManager.languageFlow
    val onboardingCompletedFlow: Flow<Boolean> = settingsManager.isOnboardingCompletedFlow
    val recentNotesFlow: Flow<List<String>> = settingsManager.recentNotesFlow

    fun saveApiKey(key: String) {
        viewModelScope.launch { settingsManager.saveApiKey(key) }
    }

    fun saveLanguage(code: String) {
        viewModelScope.launch { settingsManager.saveLanguage(code) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch { settingsManager.setOnboardingCompleted(completed) }
    }

    fun addRecentNote(note: String) {
        viewModelScope.launch { settingsManager.addRecentNote(note) }
    }

    fun deleteRecentNote(note: String) {
        viewModelScope.launch { settingsManager.deleteRecentNote(note) }
    }
}

