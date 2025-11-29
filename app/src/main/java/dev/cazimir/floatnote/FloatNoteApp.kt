package dev.cazimir.floatnote

import android.app.Application
import dev.cazimir.floatnote.data.HistoryManager
import dev.cazimir.floatnote.service.SpeechRecognitionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class FloatNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appModule = module {
            single { dev.cazimir.floatnote.data.SettingsManager(androidContext()) }
            single { HistoryManager(androidContext()) }
            single { SpeechRecognitionManager(androidContext()) }
            factory { (apiKey: String) -> dev.cazimir.floatnote.data.GeminiRepository(apiKey) }
        }

        startKoin {
            androidContext(this@FloatNoteApp)
            modules(appModule)
        }
    }
}
