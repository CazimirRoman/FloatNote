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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dev.cazimir.floatnote.data.GeminiRepository
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import dev.cazimir.floatnote.ui.theme.FloatNoteTheme
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

class FloatingBubbleService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    
    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    
    // Speech recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening by mutableStateOf(false)
    private var inputText by mutableStateOf("")
    private var errorMessage by mutableStateOf("")
    private var hasAudioPermission by mutableStateOf(false)
    private var isFormatting by mutableStateOf(false)
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()
    
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
            // Should not happen if MainActivity checks correctly, but just in case
            stopSelf()
        }
        isRunning = true
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_PERMISSION_GRANTED") {
            hasAudioPermission = true
            // If the panel is open, we might want to auto-start listening or just update the UI
            if (panelView != null) {
                errorMessage = "" // Clear any previous error
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
                            
                            val settingsManager = SettingsManager(this@FloatingBubbleService)
                            val runScope = CoroutineScope(Dispatchers.Main)
                            
                            runScope.launch {
                                val apiKey = settingsManager.apiKeyFlow.first()
                                if (apiKey.isBlank()) {
                                    errorMessage = "Gemini API Key missing. Please configure it in Settings."
                                    // Optionally, we could auto-launch settings here, but let's wait for user to read the message
                                    // or we can add a specific button in the error message area if we want to be fancy.
                                    // For now, let's just show the toast and maybe launch the activity if they try again?
                                    // Or better: Launch MainActivity directly
                                    val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        // We could add an extra to tell MainActivity to open Settings directly
                                        putExtra("OPEN_SETTINGS", true)
                                    }
                                    startActivity(intent)
                                    return@launch
                                }
                                

                                
                                isFormatting = true
                                
                                try {
                                    val repository = GeminiRepository(apiKey)
                                    val result = repository.formatText(inputText)
                                    
                                    result.onSuccess { formattedText ->
                                        inputText = formattedText
                                        errorMessage = ""
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
            
            // Handle touch outside to dismiss
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
        
        // Auto-start listening when panel opens
        checkAudioPermission()
//        if (hasAudioPermission) {
//            startListening()
//        }
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
    
    private fun startListening() {
        // Clear previous error
        errorMessage = ""
        
        // Check if RECORD_AUDIO permission is granted
        checkAudioPermission()
        if (!hasAudioPermission) {
            errorMessage = "Microphone permission not granted. Please grant permission in app settings."
            isListening = false
            return
        }
        
        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            errorMessage = "Speech recognition is not available on this device."
            isListening = false
            return
        }
        
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Use saved language code
            val settingsManager = SettingsManager(this@FloatingBubbleService)
            // We cannot block; set default and update recognizer if needed when flow emits (simple approach: read first value synchronously via runBlocking or cache)
            // For simplicity, read once using a small coroutine
        }
        
        // Read language asynchronously then start listening
        val settingsManager = SettingsManager(this)
        val scope = CoroutineScope(Dispatchers.Main)
        isListening = true
        scope.launch {
            val lang = settingsManager.languageFlow.first()
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            speechRecognizer?.startListening(intent)
        }
    }
    
    private fun stopListening() {
        if (isListening) {
            isListening = false
            speechRecognizer?.stopListening()
        }
    }
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
        }
        
        override fun onBeginningOfSpeech() {
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }
        
        override fun onEndOfSpeech() {
            // User stopped speaking
            isListening = false
        }
        
        override fun onError(error: Int) {
            // Provide detailed error messages
            errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                SpeechRecognizer.ERROR_CLIENT -> "Client side error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted."
                SpeechRecognizer.ERROR_NETWORK -> "Network error. Check internet connection."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Try again."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
                SpeechRecognizer.ERROR_SERVER -> "Server error. Try again later."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
                else -> "Recognition error occurred (code: $error)."
            }
            isListening = false
            // Light retry for transient errors
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                // Retry once after short delay
                startListening()
            }
        }
        
        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    // Append recognized text to existing input
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
        
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    // consume first to avoid empty body warning without changing UI state
                    matches.firstOrNull()?.let { _ -> /* no-op */ }
                }
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use
        }
    }
    
    private fun setupComposeView(composeView: ComposeView) {
        // Set up lifecycle owner
        composeView.setViewTreeLifecycleOwner(this)
        // Set up ViewModelStore owner
        composeView.setViewTreeViewModelStoreOwner(this)
        // Set up SavedStateRegistry owner
        composeView.setViewTreeSavedStateRegistryOwner(this)

        // Let Compose manage recomposition; avoid a custom Recomposer on the current thread.
        // Dispose composition when the view is detached to prevent leaks.
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