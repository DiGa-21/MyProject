package com.myhomechores.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Ocean,
    secondary = Sunshine,
    background = WarmBackground,
    surface = WarmBackground,
)

private val DarkColors = darkColorScheme(
    primary = OceanDark,
    secondary = Sunshine,
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

