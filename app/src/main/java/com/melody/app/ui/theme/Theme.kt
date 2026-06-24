package com.melody.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MelodyDarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentGreenLight,
    background = BackgroundDarkest,
    surface = BackgroundDark,
    onPrimary = BackgroundDarkest,
    onSecondary = BackgroundDarkest,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MelodyTheme(content: @Composable () -> Unit) {
    // 强制深色主题（音乐 APP 标配）
    MaterialTheme(
        colorScheme = MelodyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
