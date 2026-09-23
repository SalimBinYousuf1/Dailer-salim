package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreenDark,
    onPrimary = SurfaceDark,
    primaryContainer = SurfaceSecondaryDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TextSecondaryDark,
    onSecondary = SurfaceDark,
    background = SurfaceBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = SurfaceTertiaryDark,
    error = DestructiveRedDark,
    onError = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreen,
    onPrimary = SurfaceLight,
    primaryContainer = SurfaceSecondaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TextSecondaryLight,
    onSecondary = SurfaceLight,
    background = SurfaceBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceSecondaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = SurfaceTertiaryLight,
    error = DestructiveRed,
    onError = SurfaceLight
)

@Composable
fun SalimPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SalimPhoneTheme(darkTheme = darkTheme, content = content)
}
