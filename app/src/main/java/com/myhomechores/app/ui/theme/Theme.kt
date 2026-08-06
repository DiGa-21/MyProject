package com.myhomechores.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ocean,
    secondary = Sunshine,
    tertiary = PlayfulPurple,
    background = WarmBackground,
    surface = WarmBackground,
    surfaceVariant = Color(0xFFF1ECFA),
    primaryContainer = Color(0xFFD8F0F4),
    secondaryContainer = Color(0xFFFFE6A8),
    onPrimaryContainer = Color(0xFF073B4C),
    onSecondaryContainer = Color(0xFF5C4100),
)

private val DarkColors = darkColorScheme(
    primary = OceanDark,
    secondary = Sunshine,
    tertiary = Color(0xFFC4B3FF),
    background = NightBackground,
    surface = NightBackground,
)

@Composable
fun MyHomeChoresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
