package dev.cazimir.floatnote

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cazimir.floatnote.feature.bubble.FloatingBubbleService
import dev.cazimir.floatnote.feature.settings.SettingsScreen
import dev.cazimir.floatnote.core.ui.theme.FloatNoteTheme
import dev.cazimir.floatnote.feature.onboarding.OnboardingScreen
import dev.cazimir.floatnote.feature.home.MainScreen
import org.koin.androidx.compose.koinViewModel
import dev.cazimir.floatnote.ui.viewmodel.SettingsViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    
    private var hasOverlayPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
    }
    
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result is handled, service will check on next attempt
        if (!isGranted) {
            // Optionally show a message that microphone is needed for speech-to-text
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkOverlayPermission()
        
        setContent {
            FloatNoteTheme {
                val settingsViewModel: SettingsViewModel = koinViewModel()
                val isOnboardingCompleted by settingsViewModel.onboardingCompletedFlow.collectAsState(initial = true)
                val recentNotes by settingsViewModel.recentNotesFlow.collectAsState(initial = emptyList())
                val isServiceRunningState by FloatingBubbleService.serviceState.collectAsState()
                
                // Update local state when flow changes
                LaunchedEffect(isServiceRunningState) {
                    isServiceRunning = isServiceRunningState
                }
                
                var showOnboarding by remember { mutableStateOf(false) }
                
                // Sync local state with flow, but only if we haven't manually dismissed it
                LaunchedEffect(isOnboardingCompleted) {
                    showOnboarding = !isOnboardingCompleted
                }

                // Open Settings when launched with OPEN_SETTINGS extra
                LaunchedEffect(Unit) {
                    if (intent.getBooleanExtra("OPEN_SETTINGS", false)) {
                        showSettings = true
                        intent.removeExtra("OPEN_SETTINGS")
                    }
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onSaveApiKey = { key -> settingsViewModel.saveApiKey(key) },
                        onComplete = {
                            settingsViewModel.setOnboardingCompleted(true)
                            showOnboarding = false
                            // Auto-start the service after onboarding
                            startBubbleService()
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text(if (showSettings) stringResource(R.string.settings) else stringResource(R.string.app_name)) },
                                navigationIcon = {
                                    if (showSettings) {
                                        IconButton(onClick = { showSettings = false }) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (!showSettings) {
                                        IconButton(onClick = { showSettings = true }) {
                                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        if (showSettings) {
                            SettingsScreen(onNavigateBack = { showSettings = false })
                        } else {
                            MainScreen(
                                serviceState = isServiceRunning,
                                onToggleService = {
                                    if (isServiceRunning) stopBubbleService() else startBubbleService()
                                },
                                onOpenSettings = { showSettings = true },
                                recentNotes = recentNotes,
                                onDeleteNote = { note -> settingsViewModel.deleteRecentNote(note) },
                                // Legacy params for compatibility if needed by internal logic, but we use the new ones primarily
                                hasPermission = hasOverlayPermission,
                                isServiceRunning = isServiceRunning,
                                onRequestPermission = { requestOverlayPermission() },
                                onStartService = { startBubbleService() },
                                onStopService = { stopBubbleService() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_SETTINGS", false)) {
            showSettings = true
            intent.removeExtra("OPEN_SETTINGS")
        }

        // Check if we should request audio permission
        if (intent.getBooleanExtra("REQUEST_AUDIO_PERMISSION", false)) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            intent.removeExtra("REQUEST_AUDIO_PERMISSION")
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        // Sync UI with actual service state
        // isServiceRunning = FloatingBubbleService.isRunning // Removed in favor of Flow collection in setContent
    }
    
    private fun checkOverlayPermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        hasOverlayPermission = hasPermission
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun startBubbleService() {
        if (hasOverlayPermission) {
            val intent = Intent(this, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isServiceRunning = true
        }
    }
    
    private fun stopBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        stopService(intent)
        isServiceRunning = false
    }
}
