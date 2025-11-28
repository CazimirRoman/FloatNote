package dev.cazimir.floatnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setOverlayPermission(granted: Boolean) {
        _hasOverlayPermission.value = granted
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    val apiKeyFlow = settingsManager.apiKeyFlow
    val languageFlow = settingsManager.languageFlow

    fun saveLanguage(code: String) {
        viewModelScope.launch { settingsManager.saveLanguage(code) }
    }
}

