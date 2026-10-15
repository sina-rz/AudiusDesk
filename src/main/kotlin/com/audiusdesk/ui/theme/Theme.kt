package com.audiusdesk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AudiusColors {
    val Primary = Color(0xFF9B6BFF)
    val PrimaryContainer = Color(0xFF3A2A63)
    val Accent = Color(0xFF25D6E0)
    val Background = Color(0xFF0D0D14)
    val Surface = Color(0xFF14141D)
    val SurfaceVariant = Color(0xFF1D1D29)
    val Outline = Color(0xFF2E2E40)
    val TextPrimary = Color(0xFFF4F4F8)
    val TextSecondary = Color(0xFFA0A0B5)
}

private val DarkScheme = darkColorScheme(
    primary = AudiusColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AudiusColors.PrimaryContainer,
    onPrimaryContainer = Color(0xFFE8DEFF),
    secondary = AudiusColors.Accent,
    onSecondary = Color(0xFF023A3E),
    background = AudiusColors.Background,
    onBackground = AudiusColors.TextPrimary,
    surface = AudiusColors.Surface,
    onSurface = AudiusColors.TextPrimary,
    surfaceVariant = AudiusColors.SurfaceVariant,
    onSurfaceVariant = AudiusColors.TextSecondary,
    outline = AudiusColors.Outline,
    outlineVariant = Color(0xFF23232F),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF3B0007),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        shapes = AppShapes,
        content = content,
    )
}
