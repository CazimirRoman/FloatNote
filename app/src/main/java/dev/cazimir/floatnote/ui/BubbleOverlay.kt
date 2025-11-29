package dev.cazimir.floatnote.ui

import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cazimir.floatnote.data.SettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BubbleOverlay(
    context: Context,
    settingsManager: SettingsManager,
    onDrag: (Float, Float) -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isDragging) {
                    onExpand()
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    var dragJob: Job? = null
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            // Update WindowManager position
                            dragJob?.cancel()
                            dragJob = launch {
                                onDrag(dragAmount.x, dragAmount.y)
                                delay(16L)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragJob?.cancel()
                            onDismiss() // Check if we should dismiss based on final position
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
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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