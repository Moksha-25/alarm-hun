package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoltColorScheme = darkColorScheme(
    primary = VoltAccentViolet,
    onPrimary = Color(0xFF141416),
    secondary = VoltAccentBlue,
    onSecondary = Color(0xFF141416),
    background = VoltBg,
    onBackground = VoltTextPrimary,
    surface = VoltSurface,
    onSurface = VoltTextPrimary,
    surfaceVariant = VoltSurface,
    onSurfaceVariant = VoltTextSecondary,
    outline = VoltBorder,
    error = Color(0xFFFF5252)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark",
    accentColor: Color = VoltAccentViolet,
    content: @Composable () -> Unit
) {
    // VoltAlarm is strictly styled with the non-negotiable semi-dark theme
    // We dynamically apply the user's chosen accent color as the secondary color
    val dynamicColorScheme = darkColorScheme(
        primary = VoltAccentViolet,
        onPrimary = Color(0xFF141416),
        secondary = accentColor,
        onSecondary = Color(0xFF141416),
        background = VoltBg,
        onBackground = VoltTextPrimary,
        surface = VoltSurface,
        onSurface = VoltTextPrimary,
        surfaceVariant = VoltSurface,
        onSurfaceVariant = VoltTextSecondary,
        outline = VoltBorder,
        error = Color(0xFFFF5252)
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun VoltAlarmTheme(content: @Composable () -> Unit) {
    MyApplicationTheme(content = content)
}
