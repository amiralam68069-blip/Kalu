package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NovaDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianDark,
    primaryContainer = DeepViolet,
    onPrimaryContainer = TextIce,
    secondary = ElectricPurple,
    onSecondary = ObsidianDark,
    secondaryContainer = GlassSurfaceLight,
    onSecondaryContainer = TextIce,
    tertiary = NeonPink,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = DeepSpace,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextIce,
    outline = GlassBorder
)

@Composable
fun NovaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NovaTheme(content = content)
}
