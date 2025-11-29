package dev.cazimir.floatnote.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurpleLight,
    secondary = BrandPurpleLight, // Changed from orange to purple for better contrast
    tertiary = BrandOrange, // Orange as tertiary/accent
    primaryContainer = BrandPurpleDark,
    onPrimaryContainer = BrandPurpleLight,
    secondaryContainer = BrandPurple.copy(alpha = 0.2f),
    onSecondaryContainer = BrandPurpleLight,
    error = BrandOrange // Use orange for errors/warnings as accent
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    secondary = BrandPurple, // Changed from orange to purple for better contrast
    tertiary = BrandOrange, // Orange as tertiary/accent
    primaryContainer = BrandPurpleLight,
    onPrimaryContainer = BrandPurpleDark,
    secondaryContainer = BrandPurpleLight.copy(alpha = 0.3f),
    onSecondaryContainer = BrandPurpleDark,
    error = BrandOrange // Use orange for errors/warnings as accent
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