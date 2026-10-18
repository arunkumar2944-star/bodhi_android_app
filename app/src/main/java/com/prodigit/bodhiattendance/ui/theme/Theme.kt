package com.prodigit.bodhiattendance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BodhiBlue,
    onPrimary = Color.White,
    primaryContainer = BodhiBlueSoft,
    onPrimaryContainer = BodhiNavy,
    secondary = BodhiTeal,
    onSecondary = Color.White,
    secondaryContainer = BodhiTealSoft,
    onSecondaryContainer = BodhiNavy,
    tertiary = BodhiNavy,
    onTertiary = Color.White,
    background = BodhiBackground,
    onBackground = BodhiText,
    surface = BodhiSurface,
    onSurface = BodhiText,
    surfaceVariant = BodhiSurfaceMuted,
    onSurfaceVariant = BodhiTextSecondary,
    outline = BodhiOutline,
    outlineVariant = BodhiOutline,
    error = BodhiError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C6FF),
    onPrimary = Color(0xFF0B2F85),
    secondary = Color(0xFF80DCD1),
    onSecondary = Color(0xFF003D37),
    background = Color(0xFF101827),
    onBackground = Color(0xFFF0F4FA),
    surface = Color(0xFF172234),
    onSurface = Color(0xFFF0F4FA),
    surfaceVariant = Color(0xFF28364A),
    onSurfaceVariant = Color(0xFFC7D3E1),
    outline = Color(0xFF65748A)
)

@Composable
fun CRMAttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Brand consistency is important for a client-facing attendance application.
    // Do not let system wallpaper colors silently override the product's palette.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
