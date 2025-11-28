package dev.cazimir.floatnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cazimir.floatnote.data.SettingsManager
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
    val haptics = LocalHapticFeedback.current

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
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Provide a light haptic on tap
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                if (!isDragging) {
                    onTap()
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    var dragJob: Job? = null
                    var lastHapticTime = 0L
                    detectDragGestures(
                        onDragStart = {
                            isDragging = false
                            // Subtle haptic to indicate pickup
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, dragAmount ->
                            isDragging = true
                            change.consume()
                            val now = System.currentTimeMillis()
                            if (now - lastHapticTime > 120) {
                                // periodic subtle haptic during drag
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticTime = now
                            }
                            dragJob?.cancel()
                            dragJob = launch {
                                onDrag(dragAmount.x, dragAmount.y)
                                delay(16L)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = dev.cazimir.floatnote.R.drawable.ic_launcher_foreground),
            contentDescription = "FloatNote",
            modifier = Modifier.fillMaxSize()
        )

        // Flag Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-6).dp, y = (-6).dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(2.dp)
        ) {
            Text(
                text = flag,
                fontSize = 12.sp
            )
        }
    }
}