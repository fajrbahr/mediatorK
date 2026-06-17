package com.fajrbahr.mediatork.sample.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme = darkColorScheme(
    primary = Color(0xFFA6E3A1),
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF1E3A2F),
    onPrimaryContainer = Color(0xFFA6E3A1),
    surface = Color(0xFF313244),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF45475A),
    onSurfaceVariant = Color(0xFFBAC2DE),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF1E1E2E),
)

@Composable
fun PrayerTimesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
