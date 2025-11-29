package dev.cazimir.floatnote.feature.bubble

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import dev.cazimir.floatnote.core.ui.theme.FloatNoteTheme
import androidx.compose.ui.unit.dp

@Composable
fun DismissOverlay(
    isHighlighted: Boolean
) {
    val scale by animateFloatAsState(if (isHighlighted) 1.2f else 1.0f, label = "scale")
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 48.dp) // Increased padding
                .scale(scale)
                .size(110.dp) // Increased size from 80.dp
                .clip(CircleShape)
                .background(
                    if (isHighlighted) MaterialTheme.colorScheme.error.copy(alpha = 0.9f) 
                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = if (isHighlighted) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp) // Increased icon size
            )
        }
    }
}

// Light theme - normal state
@Preview(showBackground = true, name = "Dismiss Overlay")
@Composable
private fun DismissOverlayPreview() {
    FloatNoteTheme(darkTheme = false) {
        DismissOverlay(isHighlighted = false)
    }
}

// Light theme - highlighted state
@Preview(showBackground = true, name = "Dismiss Overlay Highlighted")
@Composable
private fun DismissOverlayHighlightedPreview() {
    FloatNoteTheme(darkTheme = false) {
        DismissOverlay(isHighlighted = true)
    }
}

// Dark theme - normal state
@Preview(showBackground = true, name = "Dismiss Overlay (Dark)", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DismissOverlayDarkPreview() {
    FloatNoteTheme(darkTheme = true) {
        DismissOverlay(isHighlighted = false)
    }
}

// Dark theme - highlighted state
@Preview(showBackground = true, name = "Dismiss Overlay Highlighted (Dark)", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DismissOverlayHighlightedDarkPreview() {
    FloatNoteTheme(darkTheme = true) {
        DismissOverlay(isHighlighted = true)
    }
}
