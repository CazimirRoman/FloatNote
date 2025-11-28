package dev.cazimir.floatnote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val savedApiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
    val savedLanguage by settingsManager.languageFlow.collectAsState(initial = "en-US")

    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    // Update local state when flow emits new value if not already modified
    LaunchedEffect(savedApiKey) {
        if (apiKey.isEmpty()) {
            apiKey = savedApiKey
        }
    }
    
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Language options (code, label, emoji flag)
    val languages = listOf(
        "en-US" to "English (US)" to "🇺🇸",
        "en-GB" to "English (UK)" to "🇬🇧",
        "es-ES" to "Spanish" to "🇪🇸",
        "fr-FR" to "French" to "🇫🇷",
        "de-DE" to "German" to "🇩🇪",
        "it-IT" to "Italian" to "🇮🇹",
        "pt-BR" to "Portuguese (BR)" to "🇧🇷",
        "hi-IN" to "Hindi" to "🇮🇳"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AI Configuration",
                style = MaterialTheme.typography.titleLarge
            )
            
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gemini API Key",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "Enter your Google Gemini API key to enable AI correction features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { 
                            apiKey = it 
                            showSavedMessage = false
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Text(if (isApiKeyVisible) "Hide" else "Show")
                            }
                        }
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                settingsManager.saveApiKey(apiKey)
                                showSavedMessage = true
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Key")
                    }
                    
                    if (showSavedMessage) {
                        Text(
                            text = "API Key saved successfully!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            Text(
                text = "Language",
                style = MaterialTheme.typography.titleLarge
            )

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose language for speech recognition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current selection chip
                    AssistChip(
                        onClick = {},
                        label = {
                            val selected = languages.firstOrNull { it.first.first == savedLanguage }
                            Text(text = "${selected?.second ?: "English (US)"} ${selected?.first?.second ?: "🇺🇸"}")
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null)
                        }
                    )

                    // List of languages
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(languages) { item ->
                            val (codeLabel, flag) = item.first to item.second
                            val (code, label) = codeLabel
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .clickable {
                                            scope.launch { settingsManager.saveLanguage(code) }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "$flag  $label")
                                    if (savedLanguage == code) {
                                        Text(text = "Selected", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}