package dev.cazimir.floatnote.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.cazimir.floatnote.MainActivity
import dev.cazimir.floatnote.R
import dev.cazimir.floatnote.ui.BubbleOverlay
import dev.cazimir.floatnote.ui.OverlayPanel
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import dev.cazimir.floatnote.ui.theme.FloatNoteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.koin.android.ext.android.getKoin


class FloatingBubbleService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    
    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Speech recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening by mutableStateOf(false)
    private var inputText by mutableStateOf("")
    private var currentLanguageCode = "en-US"
    private var retryAttempted = false
    
    // UI State
    private var isFormatting by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var hasAudioPermission by mutableStateOf(false)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()

    // Injected via Koin
    private val settingsManager: SettingsManager by inject()
    private val historyManager: dev.cazimir.floatnote.data.HistoryManager by inject()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore
    
    override fun onCreate() {
        super.onCreate()
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        
        ServiceCompat.startForeground(this, NOTIFICATION_ID, createNotification(), type)
        
        if (Settings.canDrawOverlays(this)) {
            createBubbleOverlay()
        } else {
            stopSelf()
        }
        isRunning = true
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_PERMISSION_GRANTED") {
            hasAudioPermission = true
            if (panelView != null) {
                errorMessage = ""
                startListening()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        stopListening()
        speechRecognizer?.destroy()
        serviceScope.cancel()
        removeBubbleOverlay()
        removePanelOverlay()
        isRunning = false
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
    
    private fun createBubbleOverlay() {
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
        
        bubbleView = ComposeView(this).apply {
            setupComposeView(this)
            
            setContent {
                FloatNoteTheme {
                    BubbleOverlay(
                        onDrag = { dx, dy ->
                            bubbleParams?.let { params ->
                                params.x += dx.toInt()
                                params.y += dy.toInt()
                                windowManager.updateViewLayout(this, params)
                            }
                        },
                        onTap = {
                            showPanelOverlay()
                        }
                    )
                }
            }
        }
        
        windowManager.addView(bubbleView, bubbleParams)
    }
    
    private fun startListening() {
        errorMessage = ""
        checkAudioPermission()
        if (!hasAudioPermission) {
            errorMessage = "Microphone permission not granted."
            return
        }
        
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
        
        val scope = CoroutineScope(Dispatchers.Main)
        isListening = true
        retryAttempted = false
        scope.launch {
            val lang = settingsManager.languageFlow.first()
            currentLanguageCode = lang
            val intent = buildRecognizerIntent(lang).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            speechRecognizer?.startListening(intent)
        }
    }

    private fun showPanelOverlay() {
        if (panelView != null) {
            removePanelOverlay()
            return
        }
        
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        panelView = ComposeView(this).apply {
            setupComposeView(this)

            setContent {
                FloatNoteTheme {
                    // Reactive language collection via SettingsManager from Koin
                    val reactiveLanguage by settingsManager.languageFlow.collectAsState(initial = currentLanguageCode)
                    androidx.compose.runtime.LaunchedEffect(reactiveLanguage) { currentLanguageCode = reactiveLanguage }

                    OverlayPanel(
                        hasAudioPermission = hasAudioPermission,
                        inputText = inputText,
                        onInputTextChange = { inputText = it },
                        isListening = isListening,
                        isFormatting = isFormatting,
                        errorMessage = errorMessage,
                        onStartListening = { startListening() },
                        onStopListening = { stopListening() },
                        onRequestPermission = { openMainActivityForPermission() },
                        onDismiss = { removePanelOverlay() },
                        onOpenSettings = {
                            val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("OPEN_SETTINGS", true)
                            }
                            startActivity(intent)
                            removePanelOverlay()
                        },
                        onFormatClick = {
                            if (inputText.isBlank()) {
                                errorMessage = "Please speak or type some text first."
                                return@OverlayPanel
                            }
                            val runScope = CoroutineScope(Dispatchers.Main)
                            runScope.launch {
                                val apiKey = settingsManager.apiKeyFlow.first()
                                if (apiKey.isBlank()) {
                                    errorMessage = "Gemini API Key missing. Please configure it in Settings."
                                    val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        putExtra("OPEN_SETTINGS", true)
                                    }
                                    startActivity(intent)
                                    return@launch
                                }
                                isFormatting = true
                                try {
                                    val repository = getKoin().get<dev.cazimir.floatnote.data.GeminiRepository> { parametersOf(apiKey) }
                                    val result = repository.formatText(inputText)
                                    result.onSuccess { formattedText ->
                                        inputText = formattedText
                                        errorMessage = ""
                                        launch(Dispatchers.IO) { historyManager.addEntry(formattedText) }
                                    }.onFailure { e ->
                                        errorMessage = "Error: ${e.localizedMessage}"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.localizedMessage}"
                                } finally {
                                    isFormatting = false
                                }
                            }
                        },
                        onCopyClick = {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FloatNote", inputText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this@FloatingBubbleService, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onShareClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, inputText)
                                type = "text/plain"
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(shareIntent)
                        }
                    )
                }
            }
            
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    removePanelOverlay()
                    true
                } else {
                    false
                }
            }
        }
        
        windowManager.addView(panelView, panelParams)
        
        checkAudioPermission()
    }
    
    private fun removeBubbleOverlay() {
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
    }
    
    private fun removePanelOverlay() {
        stopListening()
        panelView?.let {
            windowManager.removeView(it)
            panelView = null
        }
    }
    
    private fun openMainActivityForPermission() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("REQUEST_AUDIO_PERMISSION", true)
        }
        startActivity(intent)
    }
    
    private fun stopListening() {
        if (isListening) {
            isListening = false
            speechRecognizer?.stopListening()
        }
    }
    
    private fun buildRecognizerIntent(lang: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
    }
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        
        override fun onBeginningOfSpeech() {}
        
        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            val errorText = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Error $error"
            }
            
            // Soft errors - just reset
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                isListening = false
                return
            }
            
            errorMessage = errorText
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    inputText = if (inputText.isEmpty() || inputText.endsWith(" ")) {
                        "$inputText$recognizedText"
                    } else {
                        "$inputText $recognizedText"
                    }
                }
            }
            isListening = false
        }
        
        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    private fun setupComposeView(composeView: ComposeView) {
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
    
    private fun checkAudioPermission() {
        hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "FloatingBubbleChannel"
        private const val NOTIFICATION_ID = 1
        @Volatile var isRunning: Boolean = false
    }
}
