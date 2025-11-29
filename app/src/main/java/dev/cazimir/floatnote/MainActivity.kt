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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Context
import dev.cazimir.floatnote.service.FloatingBubbleService
import dev.cazimir.floatnote.ui.SettingsScreen
import dev.cazimir.floatnote.ui.theme.FloatNoteTheme
import kotlinx.coroutines.launch

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
        
        val settingsManager = dev.cazimir.floatnote.data.SettingsManager(this)

        setContent {
            FloatNoteTheme {
                val isOnboardingCompleted by settingsManager.isOnboardingCompletedFlow.collectAsState(initial = true)
                val recentNotes by settingsManager.recentNotesFlow.collectAsState(initial = emptyList())
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
                    dev.cazimir.floatnote.ui.OnboardingScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onSaveApiKey = { key -> 
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                settingsManager.saveApiKey(key)
                            }
                        },
                        onComplete = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                settingsManager.setOnboardingCompleted(true)
                            }
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
                                hasPermission = hasOverlayPermission,
                                isServiceRunning = isServiceRunning,
                                recentNotes = recentNotes,
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

@Composable
fun MainScreen(
    hasPermission: Boolean,
    isServiceRunning: Boolean,
    recentNotes: List<String>,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        ) {
            Text(
                text = "🔵", // Placeholder for icon
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Service Control Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasPermission) {
                    Text(
                        text = "Overlay Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FloatNote needs permission to display over other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Grant Permission")
                    }
                } else {
                    Text(
                        text = if (isServiceRunning) "Service Active" else "Service Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isServiceRunning) 
                            "The floating bubble is ready. Tap it to start recording." 
                        else 
                            "Start the service to enable the floating bubble.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = if (isServiceRunning) onStopService else onStartService,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else 
                                MaterialTheme.colorScheme.primary,
                            contentColor = if (isServiceRunning) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isServiceRunning) "Stop Service" else "Start Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Notes Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (recentNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recent notes yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recentNotes) { note ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Copy button
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("FloatNote", note)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                
                                // Share button
                                FilledTonalButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, note)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}