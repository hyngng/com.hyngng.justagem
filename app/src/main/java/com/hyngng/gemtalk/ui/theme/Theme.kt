package com.hyngng.gemtalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8), onPrimary = Color(0xFF002A54),
    primaryContainer = Color(0xFF1967D2), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9DBBF5), onSecondary = Color(0xFF0A1526),
    background = Color(0xFF0B0C0E), onBackground = Color(0xFFF3F5F4),
    surface = Color(0xFF101215), onSurface = Color(0xFFF3F5F4),
    surfaceVariant = Color(0xFF16191D), onSurfaceVariant = Color(0xFFA2A9B0),
    outline = Color(0xFF858D95), outlineVariant = Color(0xFF272C33),
    error = Color(0xFFFFB4AB), onError = Color(0xFF3D0908),
    errorContainer = Color(0xFF3A2024), onErrorContainer = Color(0xFFFFDAD6),
    secondaryContainer = Color(0xFF232830), onSecondaryContainer = Color(0xFFF3F5F4),
    tertiaryContainer = Color(0xFFE8F0FE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF405C91), onSecondary = Color.White,
    background = Color(0xFFF8FAF9), onBackground = Color(0xFF191D1B),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF191D1B),
    surfaceVariant = Color(0xFFEEF2F0), onSurfaceVariant = Color(0xFF49524D),
    outline = Color(0xFF68736D), outlineVariant = Color(0xFFD5DDD9),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    secondaryContainer = Color(0xFFE6EBF5), onSecondaryContainer = Color(0xFF1A2437),
    tertiaryContainer = Color(0xFFE8F0FE),
)

@Composable
fun GemTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GemTalkTypography,
        content = content,
    )
}
