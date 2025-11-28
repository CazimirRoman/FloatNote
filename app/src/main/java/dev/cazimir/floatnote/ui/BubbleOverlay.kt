package dev.cazimir.floatnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BubbleOverlay(
    onDrag: (Float, Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val language by settingsManager.languageFlow.collectAsState(initial = "en-US")

    // Map language code to flag
    val flag = remember(language) {
        when (language) {
            "en-US" -> "🇺🇸"
            "en-GB" -> "🇬🇧"
            "es-ES" -> "🇪🇸"
            "fr-FR" -> "🇫🇷"
            "de-DE" -> "🇩🇪"
            "it-IT" -> "🇮🇹"
            "pt-BR" -> "🇧🇷"
            "hi-IN" -> "🇮🇳"
            else -> "🏳️"
        }
    }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isDragging) {
                    onTap()
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    var dragJob: Job? = null
                    detectDragGestures(
                        onDragStart = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            isDragging = true
                            change.consume()
                            // Throttle updates to ~60 Hz to reduce WindowManager churn
                            dragJob?.cancel()
                            dragJob = launch {
                                onDrag(dragAmount.x, dragAmount.y)
                                delay(16L)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragJob?.cancel()
                        },
                        onDragCancel = {
                            isDragging = false
                            dragJob?.cancel()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FN",
                color = Color.White,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = flag,
                fontSize = 14.sp
            )
        }
    }
}