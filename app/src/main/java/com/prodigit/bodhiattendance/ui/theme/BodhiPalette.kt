package com.prodigit.bodhiattendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared semantic UI colors. Switches automatically with the single CRMAttendanceTheme
 * selected by Settings (LIGHT / DARK / SYSTEM). Light mode retains Bodhi branding.
 */
object BodhiPalette {
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val card: Color @Composable get() = MaterialTheme.colorScheme.surface
    val subtleCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val text: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val mutedText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val accent: Color @Composable get() = MaterialTheme.colorScheme.primary
    val accentContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
}
