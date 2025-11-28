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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cazimir.floatnote.service.FloatingBubbleService
import dev.cazimir.floatnote.ui.SettingsScreen
import dev.cazimir.floatnote.ui.theme.FloatNoteTheme

class MainActivity : ComponentActivity() {
    
    private var hasOverlayPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkOverlayPermission()

        setContent {
            FloatNoteTheme {
                // Open Settings when launched with OPEN_SETTINGS extra
                LaunchedEffect(Unit) {
                    if (intent.getBooleanExtra("OPEN_SETTINGS", false)) {
                        showSettings = true
                        intent.removeExtra("OPEN_SETTINGS")
                    }
                }

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
                            hasPermission = hasOverlayPermission,
                            isServiceRunning = isServiceRunning,
                            onRequestPermission = { requestOverlayPermission() },
                            onStartService = { startBubbleService() },
                            onStopService = { stopBubbleService() },
                            modifier = Modifier.padding(innerPadding)
                        )
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
            if (!hasOverlayPermission) {
                requestOverlayPermission()
            }
        }
        
        // Check if we should open settings (handled in Compose, but we need to ensure intent is updated)
        // The setIntent(intent) call above handles updating the intent for the Activity
    }
    
    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        // Sync UI with actual service state
        isServiceRunning = FloatingBubbleService.isRunning
    }
    
    private fun checkOverlayPermission() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
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
        stopService(Intent(this, FloatingBubbleService::class.java))
        isServiceRunning = false
    }
}

@Composable
fun MainScreen(
    hasPermission: Boolean,
    isServiceRunning: Boolean,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔵",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasPermission) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_required)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!hasPermission) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_permission))
            }
        } else {
            if (!isServiceRunning) {
                Button(
                    onClick = onStartService,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.start_bubble_service))
                }
            } else {
                Button(
                    onClick = onStopService,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.stop_bubble_service))
                }
            }
        }
        
        if (hasPermission && isServiceRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "The floating bubble is now active. You can minimize this app and the bubble will remain visible.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}