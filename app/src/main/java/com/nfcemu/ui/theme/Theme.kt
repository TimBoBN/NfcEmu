package com.nfcemu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Nocturne design system's dark ground - see the design project for the full token set.
 * Only `error`/`onError`/`errorContainer`/`onErrorContainer` are extended beyond the tokens
 * (the design system doesn't define an error color at all); everything else is a direct or
 * flattened token value.
 */
private val NocturneColors = darkColorScheme(
    background = Color(0xFF161826),
    onBackground = Color(0xFFE9E9ED),
    surface = Color(0xFF232532),
    onSurface = Color(0xFFE9E9ED),
    surfaceVariant = Color(0xFF2A2C3B),
    onSurfaceVariant = Color(0xFFA8A8B8),
    primary = Color(0xFF9184D9),
    onPrimary = Color(0xFF161826),
    primaryContainer = Color(0xFF2B2741),
    onPrimaryContainer = Color(0xFFE7E5FE),
    outline = Color(0xFF383946),
    outlineVariant = Color(0xFF383946),
    error = Color(0xFFE5484D),
    onError = Color(0xFF161826),
    errorContainer = Color(0xFF3B2429),
    onErrorContainer = Color(0xFFF4A8AC),
)

/** Retuned to the design system's own radii (4/8/14dp) instead of Material3's defaults. */
private val NfcEmuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

/** Fixed dark theme (Nocturne design) - no light variant, no Material You dynamic color. */
@Composable
fun NfcEmuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NocturneColors,
        typography = NocturneTypography,
        shapes = NfcEmuShapes,
        content = content,
    )
}
