package com.cycle.speedometer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    background = DarkBackground,
    surface = CardBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    secondary = TextMuted
)

@Composable
fun CycleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
