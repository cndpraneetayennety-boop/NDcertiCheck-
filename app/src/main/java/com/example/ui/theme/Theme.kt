package com.example.ui.theme

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
    primary = BrightCyanAccent,
    onPrimary = Color.White,
    primaryContainer = DeepNavySecondary,
    onPrimaryContainer = Color.White,
    secondary = VerifiedGreen,
    onSecondary = Color.White,
    tertiary = WarningAmber,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = DeepNavyPrimary,
    secondary = VerifiedGreen,
    onSecondary = Color.White,
    secondaryContainer = VerifiedGreenContainer,
    onSecondaryContainer = OnVerifiedGreen,
    tertiary = WarningAmber,
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = OnWarningAmber,
    error = DiscrepancyRed,
    errorContainer = DiscrepancyRedContainer,
    onErrorContainer = OnDiscrepancyRed,
    background = NeutralSurfaceLight,
    surface = NeutralCardLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional theme colors for consistent academic branding
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
