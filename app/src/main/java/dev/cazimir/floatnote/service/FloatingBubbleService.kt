package dev.cazimir.floatnote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.graphics.PixelFormat
import android.os.Build
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import dev.cazimir.floatnote.ui.theme.FloatNoteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.LaunchedEffect
import org.koin.android.ext.android.getKoin
import android.content.pm.ServiceInfo


class FloatingBubbleService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    
    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var dismissView: ComposeView? = null
    private var dismissParams: WindowManager.LayoutParams? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // UI State
    private var isDismissHighlighted by mutableStateOf(false)
    private var isFormatting by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var hasAudioPermission by mutableStateOf(false)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()

    // Injected via Koin
    private val settingsManager: SettingsManager by inject()
    private val historyManager: dev.cazimir.floatnote.data.HistoryManager by inject()
    private val speechManager: SpeechRecognitionManager by inject()

    private var currentLanguageCode: String = "en-US"
    private var isListening: Boolean by mutableStateOf(false)

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

        // Initialize manager with current language
        serviceScope.launch {
            val lang = settingsManager.languageFlow.first()
            speechManager.initialize(lang)
        }
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
        stopListeningInternal()
        serviceScope.cancel()
        removeBubbleOverlay()
        removePanelOverlay()
        removeDismissOverlay()
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
                        context = this@FloatingBubbleService,
                        settingsManager = settingsManager,
                        onDrag = { dx, dy ->
                            bubbleParams?.let { params ->
                                params.x += dx.toInt()
                                params.y += dy.toInt()
                                windowManager.updateViewLayout(this, params)
                                
                                // Check if bubble is at the bottom of the screen to dismiss
                                val screenHeight = resources.displayMetrics.heightPixels
                                val dismissThreshold = screenHeight - 200 // 200px from bottom
                                
                                if (dismissView?.visibility != android.view.View.VISIBLE) {
                                    showDismissOverlay()
                                }
                                
                                val isOverTarget = params.y > dismissThreshold
                                if (isDismissHighlighted != isOverTarget) {
                                    isDismissHighlighted = isOverTarget
                                }
                            }
                        },
                        onExpand = {
                            showPanelOverlay()
                        },
                        onDismiss = {
                            hideDismissOverlay()
                            // Check final position
                            bubbleParams?.let { params ->
                                val screenHeight = resources.displayMetrics.heightPixels
                                val dismissThreshold = screenHeight - 200
                                if (params.y > dismissThreshold) {
                                    stopSelf()
                                }
                            }
                        }
                    )
                }
            }
        }
        
        windowManager.addView(bubbleView, bubbleParams)
        createDismissOverlay()
    }
    
    private fun createDismissOverlay() {
        dismissParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        dismissView = ComposeView(this).apply {
            setupComposeView(this)
            setContent {
                FloatNoteTheme {
                    dev.cazimir.floatnote.ui.DismissOverlay(isHighlighted = isDismissHighlighted)
                }
            }
            visibility = android.view.View.GONE
        }
        
        windowManager.addView(dismissView, dismissParams)
    }
    
    private fun showDismissOverlay() {
        dismissView?.visibility = android.view.View.VISIBLE
    }
    
    private fun hideDismissOverlay() {
        dismissView?.visibility = android.view.View.GONE
        isDismissHighlighted = false
    }
    
    private fun setupComposeView(composeView: ComposeView) {
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    private fun startListening() {
        errorMessage = ""
        checkAudioPermission()
        if (!hasAudioPermission) {
            // Launch MainActivity to request permission
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("REQUEST_AUDIO_PERMISSION", true)
            }
            startActivity(intent)
            return
        }
        serviceScope.launch {
            val lang = settingsManager.languageFlow.first()
            if (lang != currentLanguageCode) {
                currentLanguageCode = lang
                speechManager.initialize(lang)
            }
            speechManager.startListening()
            isListening = true
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
                    LaunchedEffect(reactiveLanguage) { currentLanguageCode = reactiveLanguage; speechManager.initialize(reactiveLanguage) }
                    val speechState by speechManager.state.collectAsState()
                    val recognizedText by speechManager.recognizedText.collectAsState()

                    OverlayPanel(
                        hasAudioPermission = hasAudioPermission,
                        inputText = recognizedText,
                        onInputTextChange = { speechManager.updateText(it) },
                        isListening = speechState is SpeechState.Listening || speechState is SpeechState.Initializing,
                        isFormatting = isFormatting,
                        errorMessage = errorMessage,
                        onStartListening = { startListening() },
                        onStopListening = { stopListeningInternal() },
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
                            val textToFormat = recognizedText
                            if (textToFormat.isBlank()) return@OverlayPanel

                            serviceScope.launch {
                                try {
                                    isFormatting = true
                                    errorMessage = ""
                                    val apiKey = settingsManager.apiKeyFlow.first()
                                    if (apiKey.isBlank()) {
                                        errorMessage = "Please set your Gemini API key in Settings"
                                        return@launch
                                    }

                                    val repository = getKoin().get<dev.cazimir.floatnote.data.GeminiRepository> { parametersOf(apiKey) }
                                    val result = repository.formatText(textToFormat)
                                    result.onSuccess { formattedText ->
                                        speechManager.updateText(formattedText)
                                        // Save formatted text to recent notes
                                        settingsManager.addRecentNote(formattedText)
                                    }.onFailure { e ->
                                        errorMessage = "Formatting failed: ${e.message}"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Formatting failed: ${e.message}"
                                } finally {
                                    isFormatting = false
                                }
                            }
                        },
                        onCopyClick = {
                            val textToCopy = recognizedText
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FloatNote", textToCopy)
                            clipboard.setPrimaryClip(clip)
            
                            // Save to recent notes
                            serviceScope.launch {
                                settingsManager.addRecentNote(textToCopy)
                            }
            
                            Toast.makeText(this@FloatingBubbleService, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            removePanelOverlay()
                        },
                        onShareClick = {
                            val textToShare = speechManager.recognizedText.value
                            if (textToShare.isBlank()) return@OverlayPanel
            
                            // Save to recent notes
                            serviceScope.launch {
                                settingsManager.addRecentNote(textToShare)
                            }

                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                                type = "text/plain"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(shareIntent)
                            removePanelOverlay()
                        },
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
        stopListeningInternal()
        panelView?.let { windowManager.removeView(it); panelView = null }
    }

    private fun removeDismissOverlay() {
        dismissView?.let { windowManager.removeView(it); dismissView = null }
    }
    
    private fun openMainActivityForPermission() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("REQUEST_AUDIO_PERMISSION", true)
        }
        startActivity(intent)
    }
    
    private fun stopListeningInternal() {
        speechManager.stopListening()
        isListening = false
    }

    private fun checkAudioPermission() {
        hasAudioPermission = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "FloatingBubbleChannel"
        private const val NOTIFICATION_ID = 1
        
        private val _serviceState = kotlinx.coroutines.flow.MutableStateFlow(false)
        val serviceState = _serviceState.asStateFlow()
        
        var isRunning: Boolean
            get() = _serviceState.value
            set(value) { _serviceState.value = value }
    }
}
