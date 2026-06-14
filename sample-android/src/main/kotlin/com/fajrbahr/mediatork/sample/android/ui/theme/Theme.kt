package com.fajrbahr.mediatork.sample.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green800 = Color(0xFF1B6B45)
private val Green100 = Color(0xFFA8F2C6)
private val Cream = Color(0xFFF5F5EF)

private val colorScheme = lightColorScheme(
    primary = Green800,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Color(0xFF002112),
    surface = Cream,
    background = Cream,
)

@Composable
fun PrayerTimesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
