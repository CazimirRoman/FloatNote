package dev.cazimir.floatnote.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VoicenotesWhite,
    secondary = VoicenotesWhite,
    tertiary = VoicenotesRecordRed,
    background = VoicenotesBlack,
    surface = VoicenotesBlack,
    onPrimary = VoicenotesBlack,
    onSecondary = VoicenotesBlack,
    onTertiary = VoicenotesWhite,
    onBackground = VoicenotesWhite,
    onSurface = VoicenotesWhite,
    surfaceVariant = Color(0xFF1C1C1E), // Slightly lighter black for cards
    onSurfaceVariant = VoicenotesWhite,
    outline = VoicenotesDarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = VoicenotesBlack,
    secondary = VoicenotesBlack,
    tertiary = VoicenotesRecordRed,
    background = VoicenotesWhite,
    surface = VoicenotesWhite,
    onPrimary = VoicenotesWhite,
    onSecondary = VoicenotesWhite,
    onTertiary = VoicenotesWhite,
    onBackground = VoicenotesBlack,
    onSurface = VoicenotesBlack,
    surfaceVariant = VoicenotesGray, // Light gray for cards
    onSurfaceVariant = VoicenotesBlack,
    outline = VoicenotesDarkGray
)

@Composable
fun FloatNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Changed to false to use our custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}