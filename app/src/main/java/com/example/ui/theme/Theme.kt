package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleGreen,
    onPrimary = Color.White,
    primaryContainer = AppleSecondaryBackgroundDark,
    onPrimaryContainer = AppleLabelPrimaryDark,
    secondary = AppleBlue,
    onSecondary = Color.White,
    background = AppleSystemBackgroundDark,
    onBackground = AppleLabelPrimaryDark,
    surface = AppleSystemBackgroundDark,
    onSurface = AppleLabelPrimaryDark,
    surfaceVariant = AppleSecondaryBackgroundDark,
    onSurfaceVariant = AppleLabelSecondaryDark,
    outline = AppleOpaqueSeparatorDark,
    outlineVariant = AppleSeparatorDark,
    error = AppleRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AppleGreen,
    onPrimary = Color.White,
    primaryContainer = AppleSecondaryBackgroundLight,
    onPrimaryContainer = AppleLabelPrimaryLight,
    secondary = AppleBlue,
    onSecondary = Color.White,
    background = AppleSystemBackgroundLight,
    onBackground = AppleLabelPrimaryLight,
    surface = AppleSystemBackgroundLight,
    onSurface = AppleLabelPrimaryLight,
    surfaceVariant = AppleSecondaryBackgroundLight,
    onSurfaceVariant = AppleLabelSecondaryLight,
    outline = AppleOpaqueSeparatorLight,
    outlineVariant = AppleSeparatorLight,
    error = AppleRed,
    onError = Color.White
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
