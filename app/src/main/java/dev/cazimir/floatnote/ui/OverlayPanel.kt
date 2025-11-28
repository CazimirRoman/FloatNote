package dev.cazimir.floatnote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.cazimir.floatnote.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun OverlayPanel(
    hasAudioPermission: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isListening: Boolean,
    isFormatting: Boolean,
    errorMessage: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onFormatClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val maxPanelHeight = (screenHeightDp * 0.7f).dp // occupy up to 70% of screen height

    Card(
        modifier = modifier
            .widthIn(min = 320.dp, max = 420.dp)
            .heightIn(min = 240.dp, max = maxPanelHeight),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (!hasAudioPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FloatNote",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Text(
                    text = "Microphone permission not granted.\nPlease grant permission in app settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FloatNote",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        IconButton(onClick = onOpenSettings) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                // Text Input Area (larger, scrollable if long)
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 280.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("Speak or type...") }
                )

                // Controls Area
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Mic Button (Full Width)
                    Button(
                        onClick = {
                            if (isListening) {
                                onStopListening()
                            } else {
                                onStartListening()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        if (isListening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.stop_speaking))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.start_speaking))
                        }
                    }

                    // Row 2: Format, Copy, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Format Button
                        Button(
                            onClick = onFormatClick,
                            modifier = Modifier.weight(1f),
                            enabled = inputText.isNotBlank() && !isListening && !isFormatting
                        ) {
                            if (isFormatting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Format")
                            }
                        }

                        // Copy Button (Icon only)
                        OutlinedButton(
                            onClick = onCopyClick,
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Share Button (Icon only)
                        OutlinedButton(
                            onClick = onShareClick,
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Error message display (at the bottom if present)
                if (errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            if (errorMessage.contains("permission")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Grant", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Permission denied", showBackground = true)
@Composable
private fun OverlayPanelPreview_PermissionDenied() {
    MaterialTheme {
        OverlayPanel(
            hasAudioPermission = false,
            inputText = "",
            onInputTextChange = {},
            isListening = false,
            isFormatting = false,
            errorMessage = "",
            onStartListening = {},
            onStopListening = {},
            onFormatClick = {},
            onCopyClick = {},
            onShareClick = {},
            onRequestPermission = {},
            onDismiss = {},
            modifier = Modifier
        )
    }
}

@Preview(name = "Default", showBackground = true)
@Composable
private fun OverlayPanelPreview_Default() {
    MaterialTheme {
        OverlayPanel(
            hasAudioPermission = true,
            inputText = "Quick note about meeting",
            onInputTextChange = {},
            isListening = false,
            isFormatting = false,
            errorMessage = "",
            onStartListening = {},
            onStopListening = {},
            onFormatClick = {},
            onCopyClick = {},
            onShareClick = {},
            onRequestPermission = {},
            onDismiss = {},
            modifier = Modifier
        )
    }
}

@Preview(name = "Listening", showBackground = true)
@Composable
private fun OverlayPanelPreview_Listening() {
    MaterialTheme {
        OverlayPanel(
            hasAudioPermission = true,
            inputText = "",
            onInputTextChange = {},
            isListening = true,
            isFormatting = false,
            errorMessage = "",
            onStartListening = {},
            onStopListening = {},
            onFormatClick = {},
            onCopyClick = {},
            onShareClick = {},
            onRequestPermission = {},
            onDismiss = {},
            modifier = Modifier
        )
    }
}

@Preview(name = "Formatting", showBackground = true)
@Composable
private fun OverlayPanelPreview_Formatting() {
    MaterialTheme {
        OverlayPanel(
            hasAudioPermission = true,
            inputText = "raw text to be formatted",
            onInputTextChange = {},
            isListening = false,
            isFormatting = true,
            errorMessage = "",
            onStartListening = {},
            onStopListening = {},
            onFormatClick = {},
            onCopyClick = {},
            onShareClick = {},
            onRequestPermission = {},
            onDismiss = {},
            modifier = Modifier
        )
    }
}

@Preview(name = "With error", showBackground = true)
@Composable
private fun OverlayPanelPreview_WithError() {
    MaterialTheme {
        OverlayPanel(
            hasAudioPermission = true,
            inputText = "meeting notes",
            onInputTextChange = {},
            isListening = false,
            isFormatting = false,
            errorMessage = "Microphone permission required",
            onStartListening = {},
            onStopListening = {},
            onFormatClick = {},
            onCopyClick = {},
            onShareClick = {},
            onRequestPermission = {},
            onDismiss = {},
            modifier = Modifier
        )
    }
}