package com.example.ghsearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6FED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E5FF),
    secondary = Color(0xFF3D5A80),
    background = Color(0xFFF7F8FA),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF10213F),
    primaryContainer = Color(0xFF1B3A6B),
    secondary = Color(0xFF9BB8D8),
    background = Color(0xFF121418),
    surface = Color(0xFF1E2126)
)

@Composable
fun GitHubSearchTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
