package com.lughlammas.mime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MimeColors = darkColorScheme(
    primary = Color(0xFF8FBF6A),
    onPrimary = Color(0xFF121212),
    secondary = Color(0xFF769656),
    background = Color(0xFF121212),
    surface = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    error = Color(0xFFFF5555),
)

@Composable
fun MimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MimeColors,
        typography = Typography(),
        content = content,
    )
}
