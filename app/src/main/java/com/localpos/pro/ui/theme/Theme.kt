package com.localpos.pro.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LocalColors = lightColorScheme(
    primary = Color(0xFF176B4D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F4E5),
    onPrimaryContainer = Color(0xFF073B2B),
    secondary = Color(0xFFF0A43A),
    background = Color(0xFFF7F8F4),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEE8),
    error = Color(0xFFBA1A1A)
)

@Composable
fun LocalPosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LocalColors, typography = Typography(), content = content)
}
