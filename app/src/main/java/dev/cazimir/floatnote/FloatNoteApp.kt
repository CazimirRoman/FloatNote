package dev.cazimir.floatnote

import android.app.Application
import dev.cazimir.floatnote.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FloatNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@FloatNoteApp)
            modules(appModule)
        }
    }
}
