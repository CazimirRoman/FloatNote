package dev.cazimir.floatnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {
    val apiKeyFlow = settingsManager.apiKeyFlow
    val languageFlow = settingsManager.languageFlow

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsManager.saveApiKey(key)
            _saveSuccess.value = true
        }
    }

    fun saveLanguage(code: String) {
        viewModelScope.launch { settingsManager.saveLanguage(code) }
    }
}

