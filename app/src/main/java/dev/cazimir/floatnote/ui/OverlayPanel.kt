package dev.cazimir.floatnote.feature.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onDeleteClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val maxPanelHeight = (screenHeightDp * 0.85f).dp // Increased max height

    // Full screen box to handle centering and outside clicks
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // Safety padding
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() }, // Dismiss on outside click
        contentAlignment = Alignment.Center
    ) {
        // Clean Card
        Card(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 480.dp)
                .heightIn(min = 300.dp, max = maxPanelHeight)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}, // Consume clicks inside card
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Clean white/black
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Add shadow for depth
            border = null // Remove border for cleaner look
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FloatNote",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!hasAudioPermission) {
                    PermissionRequestContent(onRequestPermission)
                } else {
                    // Main Content - Flexible Height
                    Column(
                        modifier = Modifier.weight(1f), // Take available space
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Text Input Area - Clean Sheet Style
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Fill remaining height in this column
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // Very subtle background
                                .padding(8.dp)
                        ) {
                            TextField(
                                value = inputText,
                                onValueChange = onInputTextChange,
                                modifier = Modifier.fillMaxSize(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp,
                                    fontSize = 16.sp
                                ),
                                placeholder = {
                                    Text(
                                        "Tap mic to speak...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    errorContainerColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                trailingIcon = {
                                    if (inputText.isNotEmpty()) {
                                        IconButton(onClick = { onInputTextChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear text",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Action Buttons - Fixed at bottom
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Primary Action: Mic / Stop
                        Button(
                            onClick = {
                                if (isListening) onStopListening() else onStartListening()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50), // Pill shape
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening)
                                    MaterialTheme.colorScheme.tertiary // Red for recording
                                else
                                    MaterialTheme.colorScheme.primary, // Black/White
                                contentColor = if (isListening)
                                    MaterialTheme.colorScheme.onTertiary
                                else
                                    MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            AnimatedVisibility(visible = isListening) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Listening...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            AnimatedVisibility(visible = !isListening) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Start Recording",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        // Secondary Actions: Format, Copy, Share, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Format
                            IconButton(
                                onClick = onFormatClick,
                                enabled = inputText.isNotBlank() && !isListening && !isFormatting,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (isFormatting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Format",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Copy
                            IconButton(
                                onClick = onCopyClick,
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Share
                            IconButton(
                                onClick = onShareClick,
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                if (errorMessage.contains("permission", ignoreCase = true)) {
                                    TextButton(onClick = onRequestPermission) {
                                        Text("Grant", color = MaterialTheme.colorScheme.onErrorContainer)
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

@Composable
fun PermissionRequestContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = "Microphone Access Needed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "FloatNote needs access to your microphone to transcribe your speech into text.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

@Preview(name = "Default", showBackground = false)
@Composable
private fun OverlayPanelPreview_Default() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OverlayPanel(
                hasAudioPermission = true,
                inputText = "This is a sample note that I am dictating right now.",
                onInputTextChange = {},
                isListening = false,
                isFormatting = false,
                errorMessage = "",
                onStartListening = {},
                onStopListening = {},
                onFormatClick = {},
                onCopyClick = {},
                onShareClick = {},
                onDeleteClick = {},
                onRequestPermission = {},
                onDismiss = {},
                modifier = Modifier
            )
        }
    }
}

@Preview(name = "Listening", showBackground = false)
@Composable
private fun OverlayPanelPreview_Listening() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
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
                onDeleteClick = {},
                onRequestPermission = {},
                onDismiss = {},
                modifier = Modifier
            )
        }
    }
}