package dev.cazimir.floatnote

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import dev.cazimir.floatnote.core.di.appModule

class FloatNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FloatNoteApp)
            modules(appModule)
        }
    }
}
