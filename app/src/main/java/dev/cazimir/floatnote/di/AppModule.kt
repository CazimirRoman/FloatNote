package dev.cazimir.floatnote.di

import dev.cazimir.floatnote.data.CryptoManager
import dev.cazimir.floatnote.data.GeminiRepository
import dev.cazimir.floatnote.data.HistoryManager
import dev.cazimir.floatnote.data.SettingsManager
import dev.cazimir.floatnote.service.SpeechRecognitionManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import dev.cazimir.floatnote.ui.viewmodel.SettingsViewModel
import dev.cazimir.floatnote.ui.viewmodel.HistoryViewModel

val appModule = module {
    single { SettingsManager(androidContext(), get()) }
    single { HistoryManager(androidContext()) }
    single { SpeechRecognitionManager(androidContext()) }
    single { CryptoManager() }
    factory { (apiKey: String) -> GeminiRepository(apiKey) }
    viewModel { SettingsViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
}