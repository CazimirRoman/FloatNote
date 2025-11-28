package dev.cazimir.floatnote.di

import dev.cazimir.floatnote.data.GeminiRepository
import dev.cazimir.floatnote.data.SettingsManager
import dev.cazimir.floatnote.ui.MainViewModel
import dev.cazimir.floatnote.ui.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Core singletons
    single { SettingsManager(androidContext()) }
    // Parameterized repository factory; pass apiKey when needed
    factory { (apiKey: String) -> GeminiRepository(apiKey) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}

